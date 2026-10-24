package vn.edu.ptit.cinema.model.movie;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Model biểu diễn thông tin Phim
 */
public class Movie implements Serializable {
    private String id;
    private String title;
    private String genre;
    private int durationMinutes;
    private LocalDate releaseDate;
    private String posterPath;
    private String description;
    private String director;
    private String ageRating; // P, C13, C16, C18

    public Movie() {
    }

    public Movie(String id, String title, String genre, int durationMinutes, LocalDate releaseDate,
                 String posterPath, String description, String director, String ageRating) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.durationMinutes = durationMinutes;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.description = description;
        this.director = director;
        this.ageRating = ageRating;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }
}
