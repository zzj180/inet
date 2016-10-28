/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Users\\lzl\\workspace\\ParcelableTest\\src\\com\\leeon\\parcelable\\IBookManager.aidl
 */
package com.leeon.parcelable;
public interface IBookManager extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.leeon.parcelable.IBookManager
{
private static final java.lang.String DESCRIPTOR = "com.leeon.parcelable.IBookManager";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.leeon.parcelable.IBookManager interface,
 * generating a proxy if needed.
 */
public static com.leeon.parcelable.IBookManager asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.leeon.parcelable.IBookManager))) {
return ((com.leeon.parcelable.IBookManager)iin);
}
return new com.leeon.parcelable.IBookManager.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_getBookList:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<com.leeon.parcelable.Book> _result = this.getBookList();
reply.writeNoException();
reply.writeTypedList(_result);
return true;
}
case TRANSACTION_addBook:
{
data.enforceInterface(DESCRIPTOR);
com.leeon.parcelable.Book _arg0;
if ((0!=data.readInt())) {
_arg0 = com.leeon.parcelable.Book.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.addBook(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.leeon.parcelable.IBookManager
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public java.util.List<com.leeon.parcelable.Book> getBookList() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<com.leeon.parcelable.Book> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getBookList, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArrayList(com.leeon.parcelable.Book.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void addBook(com.leeon.parcelable.Book book) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((book!=null)) {
_data.writeInt(1);
book.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_addBook, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_getBookList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_addBook = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public java.util.List<com.leeon.parcelable.Book> getBookList() throws android.os.RemoteException;
public void addBook(com.leeon.parcelable.Book book) throws android.os.RemoteException;
}
