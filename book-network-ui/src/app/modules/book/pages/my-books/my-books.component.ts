import {Component, OnInit} from '@angular/core';
import {BookCardComponent} from "../../components/book-card/book-card.component";
import {PageResponseBookResponse} from "../../../../services/models/page-response-book-response";
// import {BookService} from "../../../../services/services/book.service";
import {Router, RouterLink} from "@angular/router";
import {BookResponse} from "../../../../services/models/book-response";
import {BookService} from "../../../../book/book.service";

@Component({
  selector: 'app-my-books',
  standalone: true,
  imports: [
    BookCardComponent,
    RouterLink
  ],
  templateUrl: './my-books.component.html',
  styleUrl: './my-books.component.scss'
})
export class MyBooksComponent implements OnInit{
  bookResponse: PageResponseBookResponse = {};
  page: number = 0;
  size: number = 2;


  constructor(private bookService: BookService,
              private router: Router) {

  }

  ngOnInit(): void {
    this.findAllBooks();
  }

  private findAllBooks() {
    this.bookService.findAllBooksByOwner({
      page: this.page,
      size: this.size
    }).subscribe({
      next: (books) => {
        this.bookResponse = books;
      }
    })
  }

  goToFirstPage() {
    this.page = 0;
    this.findAllBooks();
  }

  goToPreviousPage() {
    this.page--;
    this.findAllBooks();
  }

  goToPage(page: number) {
    this.page = page;
    this.findAllBooks();
  }

  goToNextPage() {
    this.page++;
    this.findAllBooks();
  }

  goToLastPage() {
    this.page = this.bookResponse.totalPages as number - 1;
    this.findAllBooks();
  }

  get isLastPage(): boolean {
    return this.page == this.bookResponse.totalPages as number - 1;
  }

  archiveBook($event: BookResponse) {

  }

  shareBook(book: BookResponse) {
    this.bookService.updateShareableStatus({
      'book-id': book.id as number
    }).subscribe({
      next:()=>{
        book.shareable= !book.shareable;
      }

    });
  }

  editBook(book: BookResponse) {
    this.router.navigate(['books','manage', book.id]);

  }
}
