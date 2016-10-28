package com.leeon.parcelable;

import com.leeon.parcelable.Book;

interface IBookManager{
	 List<Book> getBookList();
     void addBook(in Book book);
}