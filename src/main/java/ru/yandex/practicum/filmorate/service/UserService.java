package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ObjectNotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.impl.UserDbStorage;

import java.util.*;

@Service
@Slf4j
public class UserService {
    private final UserDbStorage userDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserService(UserDbStorage userDbStorage, JdbcTemplate jdbcTemplate) {
        this.userDbStorage = userDbStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Collection<User> findAll() {
        log.info("Список пользователей отправлен");

        return userDbStorage.findAll();
    }

    public User create(User user) {
        return userDbStorage.create(user);
    }

    public User update(User user) {
        return userDbStorage.update(user);
    }

    public User getById(int id) {
        return userDbStorage.getById(id);
    }

    public User deleteById(int id) {
        return userDbStorage.deleteById(id);
    }

    public List<Integer> followUser(int followingId, int followerId) {
        String check = "SELECT * FROM users WHERE id = ?";
        SqlRowSet followingRow = jdbcTemplate.queryForRowSet(check, followingId);
        SqlRowSet followerRow = jdbcTemplate.queryForRowSet(check, followerId);
        if (!followingRow.next() || !followerRow.next()) {
            log.warn("Пользователи с id {} и {} не найдены", followingId, followerId);
            throw new ObjectNotFoundException("Пользователи не найдены");
        }
        String sqlForWrite = "INSERT INTO MUTUAL_FRIENDSHIP (USER_ID, FRIEND_ID, STATUS) " +
                "VALUES (?, ?, ?)";
        String sqlForUpdate = "UPDATE MUTUAL_FRIENDSHIP SET status = ? " +
                "WHERE user_id = ? AND friend_id = ?";
        String checkMutual = "SELECT * FROM MUTUAL_FRIENDSHIP WHERE user_id = ? AND friend_id = ?";
        SqlRowSet userRows = jdbcTemplate.queryForRowSet(checkMutual, followingId, followerId);
        if (userRows.first()) {
            jdbcTemplate.update(sqlForUpdate, FriendshipStatus.CONFIRMED.toString(), followingId, followerId);
        } else {
            jdbcTemplate.update(sqlForWrite, followingId, followerId, FriendshipStatus.REQUIRED.toString());
        }
        log.info("Пользователь {} подписался на {}", followingId, followerId);

        return List.of(followingId, followerId);
    }

    public List<Integer> unfollowUser(int followingId, int followerId) {
        String sql = "DELETE FROM MUTUAL_FRIENDSHIP WHERE USER_ID = ? AND FRIEND_ID = ?";
        jdbcTemplate.update(sql, followingId, followerId);
        log.info("Пользователь {} отписался от {}", followerId, followingId);

        return List.of(followingId, followerId);
    }

    public List<User> getFriendsListById(int id) {
        String check = "SELECT * FROM users WHERE id = ?";
        SqlRowSet followingRow = jdbcTemplate.queryForRowSet(check, id);
        if (!followingRow.next()) {
            log.warn("Пользователь с id {} не найден", id);
            throw new ObjectNotFoundException("Пользователь не найден");
        }
        String sql = "SELECT id, email, login, name, birthday " +
                "FROM USERS " +
                "LEFT JOIN mutual_friendship mf on users.id = mf.friend_id " +
                //TODO тесты не предусматривают подтверждение подписки
                "where user_id = ? AND status LIKE 'REQUIRED'";
        log.info("Запрос получения списка друзей пользователя {} выполнен", id);

        return jdbcTemplate.query(sql, userDbStorage::makeUser, id);
    }

    public List<User> getCommonFriendsList(int firstId, int secondId) {
        String check = "SELECT * FROM users WHERE id = ?";
        SqlRowSet followingRow = jdbcTemplate.queryForRowSet(check, firstId);
        SqlRowSet followerRow = jdbcTemplate.queryForRowSet(check, secondId);
        if (!followingRow.next() || !followerRow.next()) {
            log.warn("Пользователи с id {} и {} не найдены", firstId, secondId);
            throw new ObjectNotFoundException("Пользователи не найдены");
        }
        String sql = "SELECT id, email, login, name, birthday " +
                "FROM mutual_friendship AS mf " +
                "LEFT JOIN users u ON u.id = mf.friend_id " +
                "WHERE mf.user_id = ? AND mf.friend_id IN ( " +
                "SELECT friend_id " +
                "FROM mutual_friendship AS mf " +
                "LEFT JOIN users AS u ON u.id = mf.friend_id " +
                "WHERE mf.user_id = ? )";
        log.info("Список общих друзей {} и {} отправлен", firstId, secondId);

        return jdbcTemplate.query(sql, userDbStorage::makeUser, firstId, secondId);
    }
}