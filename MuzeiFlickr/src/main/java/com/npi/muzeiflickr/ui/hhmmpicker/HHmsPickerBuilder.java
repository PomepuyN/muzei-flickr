package com.npi.muzeiflickr.ui.hhmmpicker;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import java.util.Vector;

/**
 * User: derek Date: 5/2/13 Time: 7:55 PM
 */
public class HHmsPickerBuilder {

    private FragmentManager manager; // Required
    private Integer styleResId; // Required
    private Fragment targetFragment;
    private int mReference;
    private Vector<HHmsPickerDialogFragment.HHmsPickerDialogHandler> mHmsPickerDialogHandlers = new Vector<HHmsPickerDialogFragment.HHmsPickerDialogHandler>();

    /**
     * Attach a FragmentManager. This is required for creation of the Fragment.
     *
     * @param manager the FragmentManager that handles the transaction
     * @return the current Builder object
     */
    public HHmsPickerBuilder setFragmentManager(FragmentManager manager) {
        this.manager = manager;
        return this;
    }

    /**
     * Attach a style resource ID for theming. This is required for creation of the Fragment. Two stock styles are
     * provided using R.style.BetterPickersDialogFragment and R.style.BetterPickersDialogFragment.Light
     *
     * @param styleResId the style resource ID to use for theming
     * @return the current Builder object
     */
    public HHmsPickerBuilder setStyleResId(int styleResId) {
        this.styleResId = styleResId;
        return this;
    }

    /**
     * Attach a target Fragment. This is optional and useful if creating a Picker within a Fragment.
     *
     * @param targetFragment the Fragment to attach to
     * @return the current Builder object
     */
    public HHmsPickerBuilder setTargetFragment(Fragment targetFragment) {
        this.targetFragment = targetFragment;
        return this;
    }

    /**
     * Attach a reference to this Picker instance. This is used to track multiple pickers, if the user wishes.
     *
     * @param reference a user-defined int intended for Picker tracking
     * @return the current Builder object
     */
    public HHmsPickerBuilder setReference(int reference) {
        this.mReference = reference;
        return this;
    }

    /**
     * Attach universal objects as additional handlers for notification when the Picker is set. For most use cases, this
     * method is not necessary as attachment to an Activity or Fragment is done automatically.  If, however, you would
     * like additional objects to subscribe to this Picker being set, attach Handlers here.
     *
     * @param handler an Object implementing the appropriate Picker Handler
     * @return the current Builder object
     */
    public HHmsPickerBuilder addHHmsPickerDialogHandler(HHmsPickerDialogFragment.HHmsPickerDialogHandler handler) {
        this.mHmsPickerDialogHandlers.add(handler);
        return this;
    }

    /**
     * Remove objects previously added as handlers.
     *
     * @param handler the Object to remove
     * @return the current Builder object
     */
    public HHmsPickerBuilder removeHHmsPickerDialogHandler(HHmsPickerDialogFragment.HHmsPickerDialogHandler handler) {
        this.mHmsPickerDialogHandlers.remove(handler);
        return this;
    }

    /**
     * Instantiate and show the Picker
     */
    public void show() {
        if (manager == null || styleResId == null) {
            Log.e("HHmsPickerBuilder", "setFragmentManager() and setStyleResId() must be called.");
            return;
        }
        final FragmentTransaction ft = manager.beginTransaction();
        final Fragment prev = manager.findFragmentByTag("hms_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        final HHmsPickerDialogFragment fragment = HHmsPickerDialogFragment.newInstance(mReference, styleResId);
        if (targetFragment != null) {
            fragment.setTargetFragment(targetFragment, 0);
        }
        fragment.setHHmsPickerDialogHandlers(mHmsPickerDialogHandlers);
        fragment.show(ft, "hms_dialog");
    }
}
