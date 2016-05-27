package me.vickychijwani.spectre.network.entity;

import java.util.Arrays;
import java.util.List;

import me.vickychijwani.spectre.model.entity.User;

// dummy wrapper class needed for Retrofit
@SuppressWarnings("unused")
public class UserList {

    public List<User> users;

    public static UserList from(User... users) {
        UserList userList = new UserList();
        userList.users = Arrays.asList(users);
        return userList;
    }

}
