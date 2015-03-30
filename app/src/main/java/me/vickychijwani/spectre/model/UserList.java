package me.vickychijwani.spectre.model;

import java.util.Arrays;
import java.util.List;

// dummy wrapper class needed for Retrofit
public class UserList {

    public List<User> users;

    public static UserList from(User... users) {
        UserList userList = new UserList();
        userList.users = Arrays.asList(users);
        return userList;
    }

}
