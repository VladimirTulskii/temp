package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ObjectNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.impl.FilmDbStorage;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class FilmService {
    private final FilmDbStorage filmDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmService(FilmDbStorage filmDbStorage, JdbcTemplate jdbcTemplate) {
        this.filmDbStorage = filmDbStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Collection<Film> findAll() {
        log.info("Список фильмов отправлен");

        return filmDbStorage.findAll();
    }

    public Film create(Film film) {
        return filmDbStorage.create(film);
    }

    public Film update(Film film) {
        return filmDbStorage.update(film);
    }

    public Film getById(int id) {
//        if (!filmStorage.getFilms().containsKey(id))
//            throw new ObjectNotFoundException("Фильм не найден");
//        log.info("Фильм с id {} отправлен", id);
//
//        return filmStorage.getById(id);
        return filmDbStorage.getById(id);
    }

    public Film deleteById(int id) {
//        if (!filmStorage.getFilms().containsKey(id))
//            throw new ObjectNotFoundException("Фильм не найден, невозможно удалить");
//        log.info("Фильм с id {} удален", id);

        return filmDbStorage.deleteById(id);
    }

    public Film addLike(int filmId, int userId) {
        String checkFilm = "SELECT * FROM films WHERE id = ?";
        String checkUser = "SELECT * FROM users WHERE id = ?";
        SqlRowSet filmRows = jdbcTemplate.queryForRowSet(checkFilm, filmId);
        SqlRowSet userRows = jdbcTemplate.queryForRowSet(checkUser, userId);
        if (!filmRows.next() || !userRows.next()) {
            log.warn("Фильм {} и(или) пользователь {} не найден.", filmId, userId);
            throw new ObjectNotFoundException("Фильм или пользователь не найдены");
        }
        String sql = "INSERT INTO films_likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);

        return filmDbStorage.getById(filmId);
    }

    public Film removeLike(int filmId, int userId) {
        String checkFilm = "SELECT * FROM films WHERE id = ?";
        String checkUser = "SELECT * FROM users WHERE id = ?";
        SqlRowSet filmRows = jdbcTemplate.queryForRowSet(checkFilm, filmId);
        SqlRowSet userRows = jdbcTemplate.queryForRowSet(checkUser, userId);
        if (!filmRows.next() || !userRows.next()) {
            log.warn("Фильм {} и(или) пользователь {} не найден.", filmId, userId);
            throw new ObjectNotFoundException("Фильм или пользователь не найдены");
        }
        String sql = "DELETE FROM films_likes " +
                "WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        log.info("Пользователь {} удалил лайк к фильму {}", userId, filmId);

        return filmDbStorage.getById(filmId);
    }

    public List<Film> getBestFilms(int count) {
//        log.info("Список самых популярных фильмов отправлен");
//
//        return filmStorage.findAll().stream()
//                .sorted((o1, o2) -> Integer.compare(o2.getUsersLikes().size(), o1.getUsersLikes().size()))
//                .limit(count)
//                .collect(Collectors.toList());
        String sql = "SELECT id, name, description, release_date, duration " +
                "FROM films " +
                "LEFT JOIN films_likes fl ON films.id = fl.film_id " +
                "group by films.id, fl.film_id IN ( " +
                "    SELECT film_id " +
                "    FROM films_likes " +
                ") " +
                "ORDER BY COUNT(fl.film_id) DESC " +
                "LIMIT ?";

        return jdbcTemplate.query(sql, filmDbStorage::makeFilm, count);
    }
}