package amiin.bazouk.application.com.demo_bytes_android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amiin.bazouk.application.com.demo_bytes_android.R;
import amiin.bazouk.application.com.demo_bytes_android.iota.Wallet;
import amiin.bazouk.application.com.demo_bytes_android.iota.AccountException;
import amiin.bazouk.application.com.demo_bytes_android.iota.TxData;

public class history_fragment extends Fragment {

    List<TxData> listTxData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.history_fragment, container, false);
        final ListView listViewTransactions = result.findViewById(R.id.list_view_transactions);
        ArrayList<Map<String, String>> listMapOfEachTransaction = new ArrayList<>();
        SimpleAdapter adapterTransactions = new SimpleAdapter(getContext(), listMapOfEachTransaction, R.layout.items_transactions,
                new String[]{"date","value"}, new int[]{R.id.date,R.id.value});

        FragmentActivity activity = getActivity();
        Thread getTransactionsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                showLoadingView();

                listTxData = new ArrayList<>();
                try {
                    listTxData = Wallet.getTransactionHistory(getContext());
                } catch (AccountException e) {
                    e.printStackTrace();
                }

                hideLoadingView();

                if(listTxData.size() <= 0) {
                    showNoTxMsg();
                    return;
                }

                for (TxData txData : listTxData) {
                    HashMap<String, String> mapOfTheNewTransaction = new HashMap<>();
                    mapOfTheNewTransaction.put("value", txData.displayIotaBal);
                    mapOfTheNewTransaction.put("date", txData.displayDate);
                    listMapOfEachTransaction.add(mapOfTheNewTransaction);
                }
                if(activity!=null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listViewTransactions.setAdapter(adapterTransactions);
                        }
                    });
                }

                listViewTransactions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setTitle("Transaction Information").setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

                        String hash = listTxData.get(position).hash;
                        String link = Wallet.getTxLink(hash);
                        String status = listTxData.get(position).persistence ? "Confirmed" : "Pending";

                        String message = "<p>"+"Hash: "+hash+"</p>"+"<br/>"
                                +"<p>"+"Status: "+status+"</p>"+"<br/>"
                                +"<p>"+"<a href=\""+link+"\">View it on explorer</a></p>";
                        Spanned messageWithLink = Html.fromHtml(message);

                        FragmentActivity fragmentActivity = getActivity();
                        if(fragmentActivity!=null) {
                            fragmentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog alertDialog = builder.setMessage(messageWithLink).create();
                                    alertDialog.show();
                                    ((TextView)alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                                }
                            });
                        }
                    }
                });
            }

            private void showNoTxMsg() {
                if(activity!=null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView empMsg = result.findViewById(R.id.empty_list_msg);
                            empMsg.setVisibility(View.VISIBLE);
                            listViewTransactions.setEmptyView(empMsg);
                        }
                    });
                }
            }

            private void hideLoadingView() {
                if(listViewTransactions.getFooterViewsCount() >0)
                {
                    View v = listViewTransactions.findViewById(R.id.loading);
                    if(v != null)
                    {
                        if(activity!=null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listViewTransactions.removeFooterView(v);
                                    listViewTransactions.setAdapter(adapterTransactions);
                                }
                            });
                        }
                    }
                }

            }

            private void showLoadingView() {
                View footer = inflater.inflate(R.layout.loading, null, false);
                listViewTransactions.addFooterView(footer);
                if(activity!=null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listViewTransactions.setAdapter(adapterTransactions);
                        }
                    });
                }
            }
        });
        getTransactionsThread.start();
        return result;
    }
}
