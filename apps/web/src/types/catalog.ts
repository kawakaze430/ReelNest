export interface Title {
  id: string;
  slug: string;
  name: string;
  synopsis: string;
  type: 'MOVIE' | 'SERIES';
  genre: string;
  language: string;
  ageRating: string;
  runtimeMinutes: number;
  releaseYear: number;
  artTone: string;
  featured: boolean;
  accessTier?: 'FREE' | 'PREMIUM';
}
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}
export interface Profile {
  id: string;
  name: string;
  preferredLanguage: 'en' | 'hi';
  child: boolean;
}
