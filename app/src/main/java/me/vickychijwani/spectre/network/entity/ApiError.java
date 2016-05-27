package me.vickychijwani.spectre.network.entity;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ApiError {

    public String message;          // meant for humans

    // unfortunately there is a bug in the field name on the API side, it is "errorType" rather than
    // "error_type", hence the custom @SerializedName
    @SerializedName("errorType")
    public String errorType;        // meant for machines: "UnauthorizedError", etc.

}
