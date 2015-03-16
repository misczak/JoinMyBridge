package com.misczak.joinmybridge;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by misczak on 3/3/15.
 */
public class PhoneBookFragment extends ListFragment {

    private static final String DIALOG_CALL = "call";

    private static final String EXTRA_BRIDGE_ID = "bridge_id";
    private static final String EXTRA_CALL_OPTIONS = "call_options";
    private static final String EXTRA_BRIDGE_NUMBER = "bridgeNumber";
    private static final String EXTRA_PARTICIPANT_CODE = "participantCode";
    private static final String EXTRA_HOST_CODE = "hostCode";


    private static final int REQUEST_CALL = 0;
    private static final int REQUEST_CONTACT = 1;
    private final int DIVIDER_HEIGHT = 10;
    private final String DEFAULT_FIELD = "None";

    private String phoneNumber;
    private ArrayList<Bridge> mBridgeList;
    private static final String TAG = "PhoneBookFragment";
    private BridgeAdapter adapter;
    private SearchView searchView;
    private MenuItem searchItem;
    private String filterString;
    private int lastPosition = -1;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.phonebook_title);
        mBridgeList = PhoneBook.get(getActivity()).getBridges();

        adapter = new BridgeAdapter(mBridgeList);
        setListAdapter(adapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setDivider(null);
        getListView().setDividerHeight(DIVIDER_HEIGHT);
        getListView().setHeaderDividersEnabled(true);
        getListView().setFooterDividersEnabled(true);
        getListView().addHeaderView(new View(getActivity()));
        getListView().addFooterView(new View(getActivity()));

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "PhoneBook onResume");
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_phonebook, menu);

        searchItem = menu.findItem(R.id.menu_item_search);
        searchView = (SearchView)searchItem.getActionView();

        SearchManager searchManager = (SearchManager)getActivity().getSystemService(Context.SEARCH_SERVICE);
        ComponentName name = getActivity().getComponentName();
        SearchableInfo searchInfo = searchManager.getSearchableInfo(name);

        searchView.setSearchableInfo(searchInfo);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                filterString = s;
                adapter.getFilter().filter(s);
                return true;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_item_import:
                Intent i = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
                i.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(i, REQUEST_CONTACT);
            case R.id.menu_item_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id){

        //Really need to double check this. Was crashing on KitKat without the minus 1, but worked
        //fine on Lollipop emulator
        //Bridge b = ((BridgeAdapter)getListAdapter()).getItem(position-1);

        /*Intent i = new Intent(getActivity(), BridgePagerActivity.class);
        i.putExtra(BridgeFragment.EXTRA_BRIDGE_ID, b.getBridgeId());
        startActivity(i);


        FragmentManager fm = getActivity().getSupportFragmentManager();
        CallDialogFragment dialog = CallDialogFragment.newInstance(b.getBridgeId());
        dialog.setTargetFragment(PhoneBookFragment.this, REQUEST_CALL);
        dialog.show(fm, DIALOG_CALL);*/
    }

    /*public void showCardOverFlowMenu(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.bridge_card_overflow, popup.getMenu());
        popup.show();
    }*/


    //Responsible for creating menu options for overflow menu on each Bridge card
    public void showCardOverFlowMenu(View v, Bridge b) {

        final Bridge bridgeCard = b;
        final View view = v;

        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.setOnMenuItemClickListener(new android.support.v7.widget.PopupMenu.OnMenuItemClickListener(){

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.menu_item_modify_bridge:

                        Intent i = new Intent(getActivity(), BridgeActivity.class);
                        i.putExtra(BridgeFragment.EXTRA_BRIDGE_ID, bridgeCard.getBridgeId());
                        startActivity(i);
                        return true;

                    case R.id.menu_item_delete_bridge:
                        String loggy2 = "Deleting bridge: " + bridgeCard.getBridgeName();
                        Log.d(TAG, loggy2);
                        PhoneBook.get(getActivity()).deleteBridge(bridgeCard);
                        mBridgeList.remove(bridgeCard);
                        adapter.remove(bridgeCard);
                        adapter.notifyDataSetChanged();


                        //Will remove bridge that is being deleted from search filter view, if active
                        if (searchView.isShown()) {
                            adapter.getFilter().filter(filterString);
                        }

                        PhoneBook.get(getActivity()).savePhoneBook();
                        return true;

                    default:
                        return false;
                }
            }

        });
        popup.inflate(R.menu.bridge_card_overflow);
        popup.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {


        UUID bridgeId;

        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_CALL) {
            boolean[] options = data.getBooleanArrayExtra(EXTRA_CALL_OPTIONS);
            bridgeId = (UUID)data.getSerializableExtra(EXTRA_BRIDGE_ID);

            Log.d(TAG, " onActivityResult arr: " + Arrays.toString(options));


            CallUtils utils = new CallUtils();

            phoneNumber = utils.getCompleteNumber(bridgeId, mBridgeList, options[0], options[1]);
            placePhoneCall(phoneNumber);

            //dial = new Intent(Intent.ACTION_CALL, Uri.parse(phoneNumber));
            //startActivity(dial);

            /*REPLACE ALL OF THIS, JUST FOR TESTING
            if (options[0] == true && options[1] == true) {
                Log.d("JOHNZZZ", "Call option 1");
                dial = new Intent(Intent.ACTION_CALL, Uri.parse("tel:11111" + bridgeId.toString()));
                startActivity(dial);

            }
            else if (options[0] == true && options[1] == false) {
                Log.d("JOHNZZZ", "Call option 2");
                dial = new Intent(Intent.ACTION_CALL, Uri.parse("tel:222222" + bridgeId.toString()));
                startActivity(dial);

            }
            else if (options[0] == false && options[1] == true) {
                Log.d("JOHNZZZ", "Call option 3");
                dial = new Intent(Intent.ACTION_CALL, Uri.parse("tel:333333" + bridgeId.toString()));
                startActivity(dial);

            }
            else {
                Log.d("JOHNZZZ", "Call option 4");
                dial = new Intent(Intent.ACTION_CALL, Uri.parse("tel:444444" + bridgeId.toString()));
                startActivity(dial);

            }*/

        }

        if (requestCode == REQUEST_CONTACT) {
            Uri contactUri = data.getData();

            String[] queryFields = {ContactsContract.CommonDataKinds.Phone.NUMBER};

            Cursor c = getActivity().getContentResolver()
                    .query(contactUri, queryFields, null, null, null);

            if (c.getCount() == 0) {
                c.close();
                return;
            }

            c.moveToFirst();
            int phoneNumberColumn = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String contactPhoneNumber = c.getString(phoneNumberColumn);
            Log.d(TAG, contactPhoneNumber);

            String delimiters = "[ ,x#*]+";
            String[] bridgeComponents = contactPhoneNumber.split(delimiters);
            int components = bridgeComponents.length;

            Intent i = new Intent(getActivity(), BridgeActivity.class);

            switch (components) {
                case 1:
                    Log.d(TAG, bridgeComponents[0]);
                    i.putExtra(BridgeFragment.EXTRA_BRIDGE_NUMBER, bridgeComponents[0]);
                    break;
                case 2:
                    Log.d(TAG, bridgeComponents[0]);
                    Log.d(TAG, bridgeComponents[1]);
                    i.putExtra(BridgeFragment.EXTRA_BRIDGE_NUMBER, bridgeComponents[0]);
                    i.putExtra(BridgeFragment.EXTRA_PARTICIPANT_CODE, bridgeComponents[1]);
                    break;
                case 3:
                    Log.d(TAG, bridgeComponents[0]);
                    Log.d(TAG, bridgeComponents[1]);
                    Log.d(TAG, bridgeComponents[2]);
                    i.putExtra(BridgeFragment.EXTRA_BRIDGE_NUMBER, bridgeComponents[0]);
                    i.putExtra(BridgeFragment.EXTRA_PARTICIPANT_CODE, bridgeComponents[1]);
                    i.putExtra(BridgeFragment.EXTRA_HOST_CODE, bridgeComponents[2]);
                    break;
                default:
                    break;
            }
            startActivityForResult(i, 0);

        }

    }

    private void placePhoneCall (String number) {

        Intent dial = new Intent(Intent.ACTION_CALL, Uri.parse(number));
        startActivity(dial);
    }



    private class BridgeAdapter extends ArrayAdapter<Bridge> {

        public BridgeAdapter(ArrayList<Bridge> bridgeList) {
            super(getActivity(), 0, bridgeList);

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.list_item_bridge4, null);
            }

            final Bridge b = getItem(position);

            TextView bridgeNameView = (TextView)convertView.findViewById(R.id.bridge_card_name);
            bridgeNameView.setText(b.getBridgeName());

            TextView bridgeNumberView = (TextView)convertView.findViewById(R.id.bridge_card_number);
            bridgeNumberView.setText("Bridge Number: " + b.getBridgeNumber());

            TextView bridgeHostCodeView = (TextView)convertView.findViewById(R.id.bridge_card_hostCode);
            if (!b.getHostCode().equals(DEFAULT_FIELD)) {
                bridgeHostCodeView.setText("Host Code: " + b.getHostCode() + b.getSecondTone());
            }
            else {
                bridgeHostCodeView.setText("Host Code: " + b.getHostCode());
            }


            TextView bridgeParticipantCodeView = (TextView)convertView.findViewById(R.id.bridge_card_participantCode);

            if (!b.getParticipantCode().equals(DEFAULT_FIELD)) {
                bridgeParticipantCodeView.setText("Participant Code: " + b.getParticipantCode() + b.getFirstTone());}
            else {
                bridgeParticipantCodeView.setText("Participant Code: " + b.getParticipantCode());
            }



            TextView bridgeCallOrder = (TextView)convertView.findViewById(R.id.bridge_call_order);
            bridgeCallOrder.setText("Code Order: " +b.getCallOrder());

            ImageView cardOverFlowMenu = (ImageView)convertView.findViewById(R.id.bridge_card_overflow);
            cardOverFlowMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    showCardOverFlowMenu(v, b);

                    final int position = getListView().getPositionForView((LinearLayout)v.getParent());


                    final Bridge b = (Bridge) getListView().getItemAtPosition(position);

                    String loggy = "Overflow menu for Position: " + position + " Bridge: " + b.getBridgeName();
                    Log.d(TAG, loggy);
                }
            });


            Button callButton = (Button)convertView.findViewById(R.id.bridge_card_callButton);
            callButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    CallDialogFragment dialog = CallDialogFragment.newInstance(b.getBridgeId());
                    dialog.setTargetFragment(PhoneBookFragment.this, REQUEST_CALL);
                    dialog.show(fm, DIALOG_CALL);
                }
            });

            /*Typeface face=Typeface.createFromAsset(getActivity().getAssets(),"fonts/Roboto-Black.ttf");
            callButton.setTypeface(face);*/

            /*Button editButton = (Button)convertView.findViewById(R.id.bridge_card_editButton);
            editButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    Intent i = new Intent(getActivity(), BridgeActivity.class);
                    i.putExtra(BridgeFragment.EXTRA_BRIDGE_ID, b.getBridgeId());
                    startActivity(i);

                }
            });*/

            Button shareButton = (Button)convertView.findViewById(R.id.bridge_card_shareButton);
            shareButton.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));

                    if (!b.getParticipantCode().equals(DEFAULT_FIELD)){
                        i.putExtra(Intent.EXTRA_TEXT, "Dial into my bridge at: " + b.getBridgeNumber() + " \n Participant Code: " + b.getParticipantCode() + b.getFirstTone());
                    }
                    else {
                        i.putExtra(Intent.EXTRA_TEXT, "Dial into my bridge at: " + b.getBridgeNumber());
                    }

                    i = Intent.createChooser(i, getString(R.string.send_bridge));
                    startActivity(i);
                }
            });

            Animation animation = AnimationUtils.loadAnimation(getContext(), (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
            convertView.startAnimation(animation);
            lastPosition = position;

            return convertView;
        }

    }

}
