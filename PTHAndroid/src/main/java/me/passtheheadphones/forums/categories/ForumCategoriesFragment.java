package me.passtheheadphones.forums.categories;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import api.forum.categories.ForumCategories;
import api.soup.MySoup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.passtheheadphones.R;
import me.passtheheadphones.callbacks.OnLoggedInCallback;
import me.passtheheadphones.callbacks.SetTitleCallback;

/**
 * Fragment for displaying the forum categories
 */
public class ForumCategoriesFragment extends Fragment implements OnLoggedInCallback,
	LoaderManager.LoaderCallbacks<ForumCategories> {
	/**
	 * List adapter displaying all the forum information
	 */
	@BindView(R.id.swipeContainer) SwipeRefreshLayout swipeContainer;

	private Unbinder unbinder;

	private ForumCategoriesListAdapter adapter;
	private ProgressBar loadingIndicator;
	private SetTitleCallback setTitle;

	public ForumCategoriesFragment(){
		//Required empty ctor
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			setTitle = (SetTitleCallback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement SetTitleCallbacks");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.fragment_list_view, container, false);
		ListView list = (ListView)view.findViewById(R.id.list);
		unbinder = ButterKnife.bind(this, view);
		loadingIndicator = (ProgressBar)view.findViewById(R.id.loading_indicator);
		adapter = new ForumCategoriesListAdapter(getActivity());
		list.setAdapter(adapter);
		list.setOnItemClickListener(adapter);
		loadingIndicator.setVisibility(View.VISIBLE);

		if (MySoup.isLoggedIn()){
			onLoggedIn();
		}

		swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				// Your code to refresh the list here.
				// Make sure you call swipeContainer.setRefreshing(false)
				// once the network request has completed successfully.
				onLoggedIn();
			}
		});
		// Configure the refreshing colors

		swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
			android.R.color.holo_green_light,
			android.R.color.holo_orange_light,
			android.R.color.holo_red_light);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		setTitle.setTitle(getString(R.string.forums));
	}

	@Override
	public void onLoggedIn(){
		if (isAdded()){
			getLoaderManager().initLoader(0, null, this);
		}
	}

	@Override
	public Loader<ForumCategories> onCreateLoader(int id, Bundle args){
		return new ForumCategoriesAsyncLoader(getActivity(), args);
	}

	@Override
	public void onLoadFinished(Loader<ForumCategories> loader, ForumCategories data){
		loadingIndicator.setVisibility(View.GONE);
		if (data == null || !data.getStatus()){
			Toast.makeText(getActivity(), "Could not load forum categories", Toast.LENGTH_LONG).show();
		}
		else if (adapter.isEmpty()){
			adapter.addAll(data.getCategories());
			adapter.notifyDataSetChanged();
		}
		swipeContainer.setRefreshing(false);
	}

	@Override
	public void onLoaderReset(Loader<ForumCategories> loader){
	}

	@Override public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}
}
