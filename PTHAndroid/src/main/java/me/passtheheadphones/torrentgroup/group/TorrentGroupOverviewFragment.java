package me.passtheheadphones.torrentgroup.group;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import api.torrents.torrents.Artist;
import api.torrents.torrents.EditionTorrents;
import api.torrents.torrents.MusicInfo;
import api.torrents.torrents.TorrentGroup;
import me.passtheheadphones.R;
import me.passtheheadphones.PTHApplication;
import me.passtheheadphones.callbacks.LoadingListener;
import me.passtheheadphones.callbacks.SetTitleCallback;
import me.passtheheadphones.callbacks.ViewArtistCallbacks;
import me.passtheheadphones.settings.SettingsActivity;
import me.passtheheadphones.views.ImageDialog;

/**
 * Fragment for viewing a torrent group's information
 */
public class TorrentGroupOverviewFragment extends Fragment implements View.OnClickListener, LoadingListener<TorrentGroup> {
	/**
	 * Torrent group being shown
	 */
	private TorrentGroup group;
	/**
	 * Callbacks to the parent activity for setting the title and viewing artists
	 */
	private SetTitleCallback setTitle;
	private ViewArtistCallbacks viewArtist;
	/**
	 * Various content views displaying the group information
	 * artistA and artistB are used to show and hide artists if there were one or two artists
	 * on the album. If there's 3+ we show various artists in A and show the listing of artists
	 */
	private ImageView image;
	private View artContainer;
	private TextView artistA, artistB;
	private ProgressBar spinner;
	private TextView albumTitle;
	private ExpandableListView torrentList;

	public TorrentGroupOverviewFragment() {
		//Required empty ctor
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			setTitle = (SetTitleCallback) activity;
			viewArtist = (ViewArtistCallbacks) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement SetTitle and ViewArtist callbacks");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.expandable_list_view, container, false);
		torrentList = (ExpandableListView) view.findViewById(R.id.exp_list);

		View imageHeader = inflater.inflate(R.layout.header_image, null);
		image = (ImageView) imageHeader.findViewById(R.id.image);
		spinner = (ProgressBar) imageHeader.findViewById(R.id.loading_indicator);
		image.setOnClickListener(this);
		artContainer = imageHeader.findViewById(R.id.art_container);

		View titleHeader = inflater.inflate(R.layout.header_album_title, null);
		artistA = (TextView) titleHeader.findViewById(R.id.artist_a);
		artistB = (TextView) titleHeader.findViewById(R.id.artist_b);
		albumTitle = (TextView) titleHeader.findViewById(R.id.title);

		torrentList.addHeaderView(imageHeader);
		torrentList.addHeaderView(titleHeader, null, false);
		if (group != null) {
			populateViews();
		}
		return view;
	}

	/**
	 * When the image in the header is clicked toggle expand/hide on it
	 */
	@Override
	public void onClick(View v) {
		if (group != null) {
			if (v.getId() == R.id.image) {
				ImageDialog dialog = ImageDialog.newInstance(group.getResponse().getGroup().getWikiImage());
				dialog.show(getChildFragmentManager(), "image_dialog");
			} else if (v.getId() == R.id.artist_a) {
				viewArtist.viewArtist(group.getResponse().getGroup().getMusicInfo().getArtists().get(0).getId().intValue());
			} else if (v.getId() == R.id.artist_b) {
				viewArtist.viewArtist(group.getResponse().getGroup().getMusicInfo().getArtists().get(1).getId().intValue());
			}
		}
	}

	/**
	 * Update all the torrent group information being shown
	 */
	public void populateViews() {
		List<EditionTorrents> editions = group.getEditions();
		setTitle.setTitle(group.getResponse().getGroup().getName());

		String imgUrl = group.getResponse().getGroup().getWikiImage();
		if (!SettingsActivity.imagesEnabled(getActivity())) {
			artContainer.setVisibility(View.GONE);
		} else {
			artContainer.setVisibility(View.VISIBLE);
			PTHApplication.loadImage(getActivity(), imgUrl, image, spinner, null, null);
		}
		albumTitle.setText(group.getResponse().getGroup().getName());

		//Choose the names for ArtistA and ArtistB or hide entirely depending on the number of artists
		TorrentGroupAdapter adapter = null;
		MusicInfo musicInfo = group.getResponse().getGroup().getMusicInfo();
		if (musicInfo == null || musicInfo.getArtists().size() > 2 || musicInfo.getArtists().isEmpty()) {
			if (torrentList.getAdapter() == null) {
				adapter = new TorrentGroupAdapter(getActivity(), getChildFragmentManager(), musicInfo, editions);
			}
			//Don't show artist name at all if there's no artist
			if (musicInfo == null || musicInfo.getArtists().isEmpty()) {
				artistA.setVisibility(View.GONE);
				artistB.setVisibility(View.GONE);
			} else {
				artistA.setText("Various Artists");
				//Change color to indicate it's not clickable
				artistA.setTextColor(getResources().getColor(android.R.color.secondary_text_dark_nodisable));
				artistB.setVisibility(View.GONE);
			}
		} else {
			artistA.setOnClickListener(this);
			artistB.setOnClickListener(this);
			List<Artist> artists = musicInfo.getAllArtists();
			if (musicInfo.getArtists().size() == 2) {
				artistA.setText(musicInfo.getArtists().get(0).getName());
				artistB.setText(musicInfo.getArtists().get(1).getName());
				if (torrentList.getAdapter() == null) {
					adapter = new TorrentGroupAdapter(getActivity(), getChildFragmentManager(),
							artists.subList(2, artists.size()), editions);
				}
			} else {
				artistA.setText(musicInfo.getArtists().get(0).getName());
				artistB.setVisibility(View.GONE);
				if (torrentList.getAdapter() == null) {
					adapter = new TorrentGroupAdapter(getActivity(), getChildFragmentManager(),
							artists.subList(1, artists.size()), editions);
				}
			}
		}
		if (adapter != null) {
			torrentList.setAdapter(adapter);
			torrentList.setOnChildClickListener(adapter);
		}
	}

	@Override
	public void onLoadingComplete(TorrentGroup group) {
		this.group = group;
		if (isAdded()) {
			populateViews();
		}
	}
}
