package me.vickychijwani.spectre.model;

import com.google.gson.annotations.SerializedName;

public class ApiError {

    public String message;          // meant for humans

    // unfortunately there is a bug in the field name on the API side, it is "errorType" rather than
    // "error_type", hence the custom @SerializedName
    @SerializedName("errorType")
    public String errorType;        // meant for machines: "UnauthorizedError", etc.

}
