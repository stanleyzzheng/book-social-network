package com.stanford.book.book;

import com.stanford.book.exception.OperationNotPermittedException;
import com.stanford.book.file.FileStorageService;
import com.stanford.book.history.BookTransactionHistory;
import com.stanford.book.history.BookTransactionHistoryRepository;
import com.stanford.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookMapper bookMapper;
    private final BookRepository bookRepository;
    private final BookTransactionHistoryRepository transactionHistoryRepository;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepository;
    private final FileStorageService fileStorageService;

    public Integer save(BookRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Book book = bookMapper.toBook(request);
        book.setOwner(user);
        return bookRepository.save(book)
                .getId();
    }

    public Integer updateBook(BookRequest request, Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(
                        () -> new EntityNotFoundException("No book found with the id:: " + bookId)
                );
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(user.getId(), book.getOwner()
                .getId())) {
            throw new OperationNotPermittedException("You cannot edit/update books that aren't yours");
        }
        Book bookUpdate = bookMapper.toBook(request);
        bookUpdate.setOwner(user);
        if (request.bookCover() == null) {
            bookUpdate.setBookCover(book.getBookCover());
        }
        System.out.println(book.getBookCover());
        return bookRepository.save(bookUpdate)
                .getId();
    }

    public BookResponse findById(Integer bookId) {
        return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(
                        () -> new EntityNotFoundException("No book found with the ID: " + bookId));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size,
                                                   Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate"));
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getId());
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size,
                                                          Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate"));
        Page<Book> books = bookRepository.findAll(BookSpecification.withOwnerId(user.getId()),
                pageable);
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size,
                                                                   Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate"));
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllBorrowedBooks(
                pageable, user.getId());
        List<BorrowedBookResponse> bookResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size,
                                                                   Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate"));
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllReturnedBooks(
                pageable, user.getId());
        List<BorrowedBookResponse> bookResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(
                        () -> new EntityNotFoundException("No Book found with the id:: " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner()
                .getId(), user.getId())) {
//             throw an exception
            throw new OperationNotPermittedException("You cannot update books shareable status");
        }
        book.setShareable(!book.isShareable());
        bookRepository.save(book);
        return bookId;
    }

    public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(
                        () -> new EntityNotFoundException("No Book found with the id:: " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner()
                .getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot update books archived status");

        }
        book.setArchived(!book.isArchived());
        bookRepository.save(book);
        return bookId;
    }

    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(
                        () -> new EntityNotFoundException("No book found with the id:: " + bookId));
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException(
                    "The requested book cannot be borrowed since it is archived or is not shareable.");
        }
        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(user.getId(), book.getOwner()
                .getId())) {
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }
        final boolean isAlreadyborrowed = transactionHistoryRepository.isAlreadyBorrowedByUser(
                bookId, user.getId());
        if (isAlreadyborrowed) {
            throw new OperationNotPermittedException("The requested book is already borrowed");
        }
        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
                .book(book)
                .returnApproved(false)
                .returned(false)
                .user(user)
                .build();
        return transactionHistoryRepository.save(bookTransactionHistory)
                .getId();


    }

    public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(
                        () -> new EntityNotFoundException("No book found with the id:: " + bookId));
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException(
                    "The requested book cannot be borrowed since it is archived or is not shareable.");
        }

        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(user.getId(), book.getOwner()
                .getId())) {
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }
        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndUserId(
                        bookId, user.getId())
                .orElseThrow(() ->
                        new OperationNotPermittedException("You did not borrow this book."));
        bookTransactionHistory.setReturned(true);
        return bookTransactionHistoryRepository.save(bookTransactionHistory)
                .getId();
    }

    public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(
                        () -> new EntityNotFoundException("No book found with the id:: " + bookId));
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException(
                    "The requested book cannot be borrowed since it is archived or is not shareable.");
        }
        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(user.getId(), book.getOwner()
                .getId())) {
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }
        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndOwnerId(
                        bookId, user.getId())
                .orElseThrow(() ->
                        new OperationNotPermittedException("This book is not returned."));
        bookTransactionHistory.setReturnApproved(true);
        return bookTransactionHistoryRepository.save(bookTransactionHistory)
                .getId();
    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser,
                                       Integer bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(
                        () -> new EntityNotFoundException("No book found with the id:: " + bookId));
        User user = ((User) connectedUser.getPrincipal());
        var bookCover = fileStorageService.saveFile(file, user.getId());
        book.setBookCover(bookCover);
        bookRepository.save(book);
    }


}
