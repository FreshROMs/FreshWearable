package xyz.tenseventyseven.fresh.wearable.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import dev.oneuiproject.oneui.widget.SelectableLinearLayout;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Contact;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.R;
import xyz.tenseventyseven.fresh.wearable.adapters.base.SelectableListAdapter;

public class ContactsListAdapter extends SelectableListAdapter<Contact, ContactsListAdapter.ViewHolder> {
    private GBDevice device;

    public interface ContactListAdapterListener extends EditStateListener {
        void onLongClickContact(Contact contact);
    }

    public ContactsListAdapter(Context context) {
        super(context);
    }

    public ContactsListAdapter(Context context, GBDevice device) {
        super(context);
        this.device = device;
    }

    @Override
    protected void onItemUpdate(Contact contact) {
        DBHelper.store(contact);
        Application.deviceService(device).onSetContacts(getItems());
    }

    @Override
    protected void updateEditModeViews(ViewHolder holder, int position) {
        Contact contact = items.get(position);
        holder.selector.setSelectionMode(isEditMode);
        holder.selector.setSelectedAnimate(selectedItems.contains(contact));
    }

    @NonNull
    @Override
    public ContactsListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wear_contact_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Contact contact = items.get(position);
        holder.name.setText(contact.getName());
        holder.number.setText(contact.getNumber());

        holder.selector.setOnClickListener(null);
        holder.selector.setOnLongClickListener(null);

        holder.selector.setOnClickListener(v -> {
            if (isEditMode) {
                boolean selected = !selectedItems.contains(contact);
                holder.selector.setSelectedAnimate(selected);
                if (selected) {
                    selectedItems.add(contact);
                } else {
                    selectedItems.remove(contact);
                }
                notifySelectionChanged();
            }
        });
        holder.selector.setOnLongClickListener(v -> {
            if (listener instanceof ContactListAdapterListener) {
                ((ContactListAdapterListener) listener).onLongClickContact(contact);
            }
            return true;
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final SelectableLinearLayout selector;
        private final ImageView icon;
        private final TextView name;
        private final TextView number;

        public ViewHolder(View itemView) {
            super(itemView);
            selector = itemView.findViewById(R.id.selectable_layout);
            icon = itemView.findViewById(R.id.contact_item_icon);
            name = itemView.findViewById(R.id.contact_item_name);
            number = itemView.findViewById(R.id.contact_item_number);
        }
    }
}
