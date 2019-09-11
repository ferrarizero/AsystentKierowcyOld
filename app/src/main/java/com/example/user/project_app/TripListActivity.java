package com.example.user.project_app;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.user.project_app.R;
import com.example.user.project_app.ConfirmDialog;
import com.example.user.project_app.trips.TripListAdapter;
import com.example.user.project_app.trips.TripLog;
import com.example.user.project_app.trips.TripRecord;

import java.util.List;

import roboguice.activity.RoboActivity;

import static com.example.user.project_app.ConfirmDialog.createDialog;

public class TripListActivity extends RoboActivity implements ConfirmDialog.Listener {

    private List<TripRecord> records;
    private TripLog triplog = null;
    private TripListAdapter adapter = null;

    /// the currently selected row from the list of records
    private int selectedRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trips_list);

        ListView lv = (ListView) findViewById(R.id.tripList);

        triplog = TripLog.getInstance(this.getApplicationContext());
        records = triplog.readAllRecords();
        adapter = new TripListAdapter(this, records);
        lv.setAdapter(adapter);
        registerForContextMenu(lv);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        // create the menu
        getMenuInflater().inflate(R.menu.context_trip_list, menu);

        // get index of currently selected row
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedRow = (int) info.id;

        // get record that is currently selected
        TripRecord record = records.get(selectedRow);
    }


    public boolean onContextItemSelected(MenuItem item) {

        // get index of currently selected row
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        selectedRow = (int) info.id;

        switch (item.getItemId()) {
            case R.id.itemDelete:
                showDialog(ConfirmDialog.DIALOG_CONFIRM_DELETE_ID);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    protected Dialog onCreateDialog(int id) {
        return createDialog(id, this, this);
    }


    protected void deleteRow() {

        // get the record to delete from our list of records
        TripRecord record = records.get(selectedRow);

        // attempt to remove the record from the log
        if (triplog.deleteTrip(record.getID())) {
            records.remove(selectedRow);
            adapter.notifyDataSetChanged();
        } else {
            //Utilities.toast(this,getString(R.string.toast_delete_failed));
        }
    }

    @Override
    public void onConfirmationDialogResponse(int id, boolean confirmed) {
        removeDialog(id);
        if (!confirmed) return;

        switch (id) {
            case ConfirmDialog.DIALOG_CONFIRM_DELETE_ID:
                deleteRow();
                break;

            default:
                //Utilities.toast(this,"Invalid dialog id.");
        }

    }
}
