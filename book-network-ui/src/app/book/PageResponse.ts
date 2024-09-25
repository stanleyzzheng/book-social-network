import {BookResponse} from "../services/models/book-response";

export interface PageResponse<T> {
  content?: Array<T>;
  first?: boolean;
  last?: boolean;
  number?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}
