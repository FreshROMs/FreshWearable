package xyz.tenseventyseven.fresh.wearable.activities.devicesettings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import dev.oneuiproject.oneui.utils.ItemDecorRule;
import dev.oneuiproject.oneui.utils.SemItemDecoration;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Contact;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.common.AbstractNoActionBarActivity;
import xyz.tenseventyseven.fresh.databinding.WearActivityContactsBinding;
import xyz.tenseventyseven.fresh.wearable.adapters.ContactsListAdapter;

public class ContactsActivity extends AbstractNoActionBarActivity implements MenuProvider, ToolbarLayout.ActionModeListener, ContactsListAdapter.ContactListAdapterListener {
    private static final int PICK_CONTACT_REQUEST = 1;
    private WearActivityContactsBinding binding;
    private ContactsListAdapter adapter;
    private GBDevice device;
    private boolean isSyncDisabled;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = WearActivityContactsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            finish();
            return;
        }

        adapter = new ContactsListAdapter(this, device);
        adapter.setListener(this);
        adapter.getSelectionState().observe(this, (state) -> binding.toolbar.updateAllSelector(state.getCount(), true, state.isAllSelected()));

        binding.contactsList.setLayoutManager(new LinearLayoutManager(this));
        binding.contactsList.setAdapter(adapter);

        SemItemDecoration decoration = new SemItemDecoration(
                this,
                ItemDecorRule.ALL.INSTANCE,
                ItemDecorRule.NONE.INSTANCE
        );
        decoration.setDividerInsetStart(getResources().getDimensionPixelSize(R.dimen.wear_contact_list_divider_inset_start));
        binding.contactsList.addItemDecoration(decoration);
        binding.swipeRefreshLayout.setRefreshing(true);
        binding.swipeRefreshLayout.setOnRefreshListener(this::requestUpdate);

        binding.toolbar.setTitle(getString(R.string.wear_device_contacts_title));
        addMenuProvider(this);

        requestUpdate();
    }

    @Override
    protected void onPause() {
        if (!isSyncDisabled && device.isInitialized()) {
            sendContactsToDevice();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                try {
                    String name = null;
                    String number = null;

                    // Get the contact URI
                    android.net.Uri contactUri = data.getData();

                    // Query for phone number
                    String[] projection = new String[]{
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    };

                    try (android.database.Cursor cursor = getContentResolver().query(
                            contactUri, projection, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

                            if (numberIndex >= 0) {
                                number = cursor.getString(numberIndex);
                            }

                            if (nameIndex >= 0) {
                                name = cursor.getString(nameIndex);
                            }

                            // Now create and add the contact to your database
                            if (name != null && number != null) {
                                createNewContact(name, number);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void createNewContact(String name, String number) {
        // Create a new Contact entity
        Contact contact = new Contact();
        contact.setName(name);
        contact.setNumber(number);
        contact.setContactId(UUID.randomUUID().toString());

        // Store it in the database
        DBHelper.store(device, contact);

        // Update the UI
        updateContactsFromDB();

        // Mark that we need to sync to device
        isSyncDisabled = false;
        sendContactsToDevice();
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            binding.toolbar.endActionMode();
            return;
        }

        super.onBackPressed();
    }

    private void requestUpdate() {
        // For UX reasons, wait 1 second before requesting contacts
        // binding.swipeRefreshLayout.postDelayed(() -> Application.deviceService(device).onRequestAlarms(), 1000);
        updateContactsFromDB();
    }

    private void onAddContact() {
        Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(pickContact, PICK_CONTACT_REQUEST);
    }

    private void sendContactsToDevice() {
        ArrayList<Contact> contacts = adapter.getItems();
        Application.deviceService(device).onSetContacts(contacts);
    }

    private void updateContactsFromDB() {
        List<Contact> contacts = DBHelper.getContacts(device);

        // Sort contacts by name
        contacts.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        adapter.setItems(contacts);
        binding.swipeRefreshLayout.setRefreshing(false);
        if (contacts.isEmpty()) {
            binding.contactNoItems.setVisibility(View.VISIBLE);
            binding.contactsList.setVisibility(View.GONE);
        } else {
            binding.contactsList.setVisibility(View.VISIBLE);
            binding.contactNoItems.setVisibility(View.GONE);
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.wear_list_menu, menu);
        menu.findItem(R.id.action_add).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        boolean enabled = !binding.swipeRefreshLayout.isRefreshing() && device.getDeviceCoordinator().getContactsSlotCount(device) > adapter.getItemCount();
        menu.findItem(R.id.action_add).setEnabled(enabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menu.findItem(R.id.action_add).setIconTintList(getColorStateList(enabled ? R.color.wearable_header_title : R.color.wearable_secondary_text));
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.action_add) {
            onAddContact();
            return true;
        } else if (id == R.id.action_edit) {
            adapter.setEditMode(true);
            return true;
        }
        return false;
    }

    @Override
    public void onInflateActionMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.wear_contacts_edit_menu, menu);
        menu.findItem(R.id.action_delete).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_WITH_TEXT|MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public void onEndActionMode() {
        adapter.setEditMode(false);
    }

    @Override
    public boolean onMenuItemClicked(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.action_delete) {
            List<Contact> selectedItems = adapter.getSelectedItems();
            for (Contact contact : selectedItems) {
                DBHelper.delete(contact);
            }
        }

        binding.toolbar.endActionMode();
        binding.swipeRefreshLayout.setRefreshing(true);
        requestUpdate();
        return true;
    }

    @Override
    public void onSelectAll(boolean enabled) {
        adapter.setEditMode(true, enabled);
        binding.toolbar.updateAllSelector(enabled ? adapter.getItems().size() : 0, true, enabled);
    }

    @Override
    public void onLongClickContact(Contact contact) {
        adapter.setEditMode(contact);
    }

    @Override
    public void onEditStateChange(boolean isEditMode) {
        this.isEditMode = isEditMode;
        if (isEditMode) {
            binding.toolbar.startActionMode(this);
        } else {
            sendContactsToDevice();
        }
    }
}