package info.guardianproject.securereaderinterface.models;

import android.os.AsyncTask;

import com.tinymission.rss.MediaContent;

import java.io.Serializable;
import java.lang.ref.WeakReference;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereader.SimpleSyncTaskMediaFetcherCallback;
import info.guardianproject.securereader.SyncTaskMediaFetcher;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.adapters.DownloadsAdapter;
import info.guardianproject.securereaderinterface.views.media.MediaPreviewLayout;

/**
 * Created by N-Pex on 2017-01-31.
 */

public class MediaItemViewModel implements Serializable {
    public final MediaContent media;
    private transient final ItemViewModel ownerItem;
    private boolean downloaded;
    private boolean downloading;
    private File file;

    public MediaItemViewModel(ItemViewModel ownerItem, MediaContent media) {
        this.ownerItem = ownerItem;
        this.media = media;

        // Get initial download state by calling socialReader.
        App.getInstance().socialReader.loadMediaContent(ownerItem.item, media, new SimpleSyncTaskMediaFetcherCallback() {
            @Override
            public void mediaDownloaded(MediaContent mediaContent, File mediaFile) {
                file = mediaFile;
                downloaded = true;
            }
        }, false, false);
    }

    public boolean getMediaData(MediaPreviewLayout previewView, boolean forceDownload) {
        synchronized (media) {
            if (isDownloaded()) {
                if (previewView != null) {
                    previewView.onDataAvailable();
                }
                return true;
            }
        }
        previewView.onDataDownloading();
        GetMediaWorkerTask getMediaWorkerTask = new GetMediaWorkerTask(previewView);
        getMediaWorkerTask.execute(forceDownload);
        return false;
    }

    public void download() {
        getMediaData(null, true);
    }

    public boolean isDownloaded() {
        synchronized (media) {
            return downloaded;
        }
    }

    public boolean isDownloading() {
        synchronized (media) {
            return downloading;
        }
    }

    public File getFile() {
        synchronized (media) {
            return file;
        }
    }

    private class GetMediaWorkerTask extends AsyncTask<Boolean, Void, Boolean> {
        private WeakReference<MediaPreviewLayout> previewViewReference;

        GetMediaWorkerTask(MediaPreviewLayout previewView) {
            if (previewView != null) {
                previewViewReference = new WeakReference<>(previewView);
            }
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            boolean forceDownload = params[0];
    		return App.getInstance().socialReader.loadMediaContent(ownerItem.item, media, new SimpleSyncTaskMediaFetcherCallback() {
	    		@Override
		    	public void mediaDownloaded(MediaContent mediaContent, File mediaFile) {
                    if (mediaFile != null) {
                        synchronized (media) {
                            downloaded = true;
                            downloading = false;
                            file = mediaFile;
                        }
                        onDownloaded();
                    }
			    }

                @Override
                public void mediaDownloadError(MediaContent mediaContent) {
                    onDownloadError();
                }
            }, true, forceDownload);
        }

        @Override
        protected void onPostExecute(Boolean downloadedOrDownloading) {
            if (downloadedOrDownloading) {
                if (!downloaded) {
                    downloading = true;
                    DownloadsAdapter.downloading(ownerItem, MediaItemViewModel.this);
                }
            }
        }

        // Do we have data (and still a view component that is alive?)
        private void onDownloaded() {
            if (downloaded) {
                DownloadsAdapter.downloaded(ownerItem, MediaItemViewModel.this);
            }
            if (previewViewReference != null && file != null) {
                final MediaPreviewLayout previewView = previewViewReference.get();
                if (previewView != null) {
                    previewView.post(new Runnable() {
                        @Override
                        public void run() {
                            previewView.onDataDownloaded();
                            previewView.onDataAvailable();
                        }
                    });
                }
            }
        }

        private void onDownloadError() {
            if (previewViewReference != null) {
                final MediaPreviewLayout previewView = previewViewReference.get();
                if (previewView != null) {
                    previewView.post(new Runnable() {
                        @Override
                        public void run() {
                            previewView.onDataDownloadError();
                        }
                    });
                }
            }
        }
    }
}
