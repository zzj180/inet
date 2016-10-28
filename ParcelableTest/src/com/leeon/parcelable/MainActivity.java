package com.leeon.parcelable;

import java.util.List;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity {
	
	private IBookManager mIBookManager;
	
	private ServiceConnection mBookManagerServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mIBookManager = IBookManager.Stub.asInterface(service);
			try {
				List<Book> book = mIBookManager.getBookList();
				for (Book value : book) {
					Log.i("book", value.toString());
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mIBookManager = null;
		}
		
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, BookManagerService.class);
        bindService(intent,mBookManagerServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onDestroy() {
    	unbindService(mBookManagerServiceConnection);
    	super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
