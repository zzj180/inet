package com.leeon.parcelable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class BookManagerService extends Service {
	
	private AtomicBoolean mIsServiceDestoryed = new AtomicBoolean(false);
	private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<Book>();

	private Binder mBinder = new IBookManager.Stub() {
		
		@Override
		public List<Book> getBookList() throws RemoteException {
			return mBookList;
		}
		
		@Override
		public void addBook(Book book) throws RemoteException {
			mBookList.add(book);
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("book","service onCreate()");
		mBookList.add(new Book(0,"android"));
		mBookList.add(new Book(1,"IOS"));
	}
	
	@Override
	public void onDestroy() {
		mIsServiceDestoryed.set(true);
		Log.i("book","service onDestroy()");
		super.onDestroy();
	}
	
	
	
}
