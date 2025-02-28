package xyz.tenseventyseven.fresh.wearable.adapters.base;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public abstract class SelectableListAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected static final String PAYLOAD_EDIT_MODE = "PAYLOAD_EDIT_MODE";
    protected final Context context;
    protected ArrayList<T> items;
    protected final ArrayList<T> selectedItems = new ArrayList<>();
    protected boolean isEditMode = false;

    public static class SelectionState {
        private final int count;
        private final boolean allSelected;

        public SelectionState(int count, boolean allSelected) {
            this.count = count;
            this.allSelected = allSelected;
        }

        public int getCount() {
            return count;
        }

        public boolean isAllSelected() {
            return allSelected;
        }
    }
    protected MutableLiveData<SelectionState> stateData = new MutableLiveData<>();

    public interface EditStateListener {
        void onEditStateChange(boolean isEditMode);
    }
    
    protected EditStateListener listener;

    public SelectableListAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setItems(List<T> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    public ArrayList<T> getItems() {
        return items;
    }

    public ArrayList<T> getSelectedItems() {
        return selectedItems;
    }

    public MutableLiveData<SelectionState> getSelectionState() {
        return stateData;
    }

    protected void setEditModeInternal(boolean isEditMode, boolean selectAll) {
        this.isEditMode = isEditMode;
        if (selectAll) {
            selectedItems.addAll(items);
        }
        
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_EDIT_MODE);
        notifySelectionChanged();
        if (listener != null) {
            listener.onEditStateChange(isEditMode);
        }
    }

    public void setEditMode(boolean isEditMode, boolean selectAll) {
        selectedItems.clear();
        setEditModeInternal(isEditMode, selectAll);
    }

    public void setEditMode(boolean isEditMode) {
        setEditMode(isEditMode, false);
    }

    public void setEditMode(T item) {
        selectedItems.clear();
        selectedItems.add(item);
        setEditModeInternal(true, false);
    }

    public void setListener(EditStateListener listener) {
        this.listener = listener;
    }

    protected void notifySelectionChanged() {
        stateData.setValue(new SelectionState(selectedItems.size(), selectedItems.size() == items.size()));
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            for (Object payload : payloads) {
                if (PAYLOAD_EDIT_MODE.equals(payload)) {
                    updateEditModeViews(holder, position);
                    return;
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    // Methods to be implemented by subclasses
    protected abstract void updateEditModeViews(VH holder, int position);
    protected abstract void onItemUpdate(T item);
}