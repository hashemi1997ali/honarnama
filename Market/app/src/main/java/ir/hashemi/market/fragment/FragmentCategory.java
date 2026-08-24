package ir.hashemi.market.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import ir.hashemi.market.ActivityCategoryDetails;
import ir.hashemi.market.ActivityAuctionDetails;
import ir.hashemi.market.ActivityMain;
import ir.hashemi.market.R;
import ir.hashemi.market.adapter.AdapterCategory;
import ir.hashemi.market.connection.API;
import ir.hashemi.market.connection.RestAdapter;
import ir.hashemi.market.connection.callbacks.CallbackCategory;
import ir.hashemi.market.model.Category;
import ir.hashemi.market.utils.CategoryIcons;
import ir.hashemi.market.utils.NetworkCheck;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentCategory extends Fragment {

    private View root_view;
    private RecyclerView recyclerView;
    private Call<CallbackCategory> callbackCall;
    private AdapterCategory adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_category, null);
        initComponent();
        requestListCategory();

        return root_view;
    }

    private void initComponent() {
        recyclerView = (RecyclerView) root_view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1));

        //set data and list adapter
        adapter = new AdapterCategory(new ArrayList<Category>());
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setVisibility(View.GONE);

        adapter.setOnItemClickListener(new AdapterCategory.OnItemClickListener() {
            @Override
            public void onItemClick(View view, Category obj) {
                if (CategoryIcons.isAuction(obj.name, obj.icon)) {
                    startActivity(new Intent(getActivity(), ActivityAuctionDetails.class));
                    return;
                }
                Snackbar.make(root_view, obj.name, Snackbar.LENGTH_SHORT).show();
                ActivityCategoryDetails.navigate(getActivity(), obj);
            }
        });
    }


    private void requestListCategory() {
        API api = RestAdapter.createAPI();
        callbackCall = api.getListCategory();
        callbackCall.enqueue(new Callback<CallbackCategory>() {
            @Override
            public void onResponse(Call<CallbackCategory> call, Response<CallbackCategory> response) {
                CallbackCategory resp = response.body();
                if (resp != null && resp.status.equals("success")) {
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setItems(resp.categories);
                    ActivityMain.getInstance().category_load = true;
                    ActivityMain.getInstance().showDataLoaded();
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(Call<CallbackCategory> call, Throwable t) {
                Log.e("onFailure", t.getMessage());
                if (!call.isCanceled()) onFailRequest();
            }

        });
    }

    private void onFailRequest() {
        if (NetworkCheck.isConnect(getActivity())) {
            showFailedView(R.string.msg_failed_load_data);
        } else {
            showFailedView(R.string.no_internet_text);
        }
    }

    private void showFailedView(@StringRes int message) {
        ActivityMain.getInstance().showDialogFailed(message);
    }

}
