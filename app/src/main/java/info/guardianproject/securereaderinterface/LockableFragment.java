package info.guardianproject.securereaderinterface;

import android.content.Intent;

public interface LockableFragment 
{
	void onUnlockedActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent);
}
