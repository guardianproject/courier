package info.guardianproject.securereaderinterface.views.media;

import com.tinymission.rss.MediaContent;

/**
 * Created by N-Pex on 2017-02-02.
 */

public interface MediaPreviewView {
    void setDataSource(MediaContent mediaContent, info.guardianproject.iocipher.File mediaFile);
    void recycle();
}
