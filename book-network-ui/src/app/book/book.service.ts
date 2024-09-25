import { Injectable } from '@angular/core';
import {environment} from "../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {BookResponse} from "../services/models/book-response";
import {BookRequest} from "../services/models/book-request";
import {PageResponse} from "./PageResponse";

@Injectable({
  providedIn: 'root'
})
export class BookService {
  private rootUrl = environment.rootUrl +"/books";

  constructor(
    private http: HttpClient
  ) {
  }

  saveBook(params: {body:BookRequest}): Observable<number>{
    const url = `${this.rootUrl}`
    return this.http.post<number>(url, params.body);
  }

  findBookById(params:{'book-id':any}): Observable<BookResponse>{
    const url = `${this.rootUrl}/${params['book-id']}`
    return this.http.get<BookResponse>(url);
  }

  uploadBookCoverPicture(params: { "book-id": number; body: { file: any } }):Observable<BookResponse> {
    const url = `${this.rootUrl}/cover/${params["book-id"]}`
    const formData = new FormData();
    formData.append('file', params.body.file);
    return this.http.post<BookResponse>(url,formData);
  }

  findAllBooksByOwner(params: { size: number; page: number }):Observable<PageResponse<BookResponse>> {
    const url = `${this.rootUrl}/owner`
    return this.http.get<PageResponse<BookResponse>>(url, {params} )
  }

  updateShareableStatus(params: { 'book-id': number } ):Observable<number> {
    const url = `${this.rootUrl}/shareable/${params['book-id']}`;
    // const httpParams = new HttpParams().set('book-id', param["book-id"].toString());
    return this.http.patch<number>(url,{});
  }
}
