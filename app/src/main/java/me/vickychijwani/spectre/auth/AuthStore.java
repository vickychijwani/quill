package me.vickychijwani.spectre.auth;

import io.reactivex.Observable;
import me.vickychijwani.spectre.account.AccountManager;
import me.vickychijwani.spectre.model.entity.BlogMetadata;
import me.vickychijwani.spectre.network.entity.AuthReqBody;
import me.vickychijwani.spectre.util.Pair;

public class AuthStore implements CredentialSource, CredentialSink {

    // credential source
    @Override
    public Observable<String> getGhostAuthCode(GhostAuth.Params params) {
        BlogMetadata blog = AccountManager.getBlog(params.blogUrl);
        return Observable.just(blog.getAuthCode());
    }

    @Override
    public Observable<Pair<String, String>> getEmailAndPassword(PasswordAuth.Params params) {
        BlogMetadata blog = AccountManager.getBlog(params.blogUrl);
        return Observable.just(new Pair<>(blog.getEmail(), blog.getPassword()));
    }

    // credential sink
    @Override
    public void saveCredentials(String blogUrl, AuthReqBody authReqBody) {
        BlogMetadata blog;
        if (AccountManager.hasBlog(blogUrl)) {
            blog = AccountManager.getBlog(blogUrl);
        } else {
            blog = new BlogMetadata();
            blog.setBlogUrl(blogUrl);
        }

        if (authReqBody.isGrantTypePassword()) {
            blog.setEmail(authReqBody.email);
            blog.setPassword(authReqBody.password);
        } else {
            blog.setAuthCode(authReqBody.authorizationCode);
        }
        blog.setLoggedIn(true);
        AccountManager.addOrUpdateBlog(blog);
    }

    @Override
    public void deleteCredentials(String blogUrl) {
        BlogMetadata blog = AccountManager.getBlog(blogUrl);
        blog.setEmail(null);
        blog.setPassword(null);
        blog.setAuthCode(null);
        blog.setLoggedIn(false);
        AccountManager.addOrUpdateBlog(blog);
    }

    @Override
    public void setLoggedIn(String blogUrl, boolean isLoggedIn) {
        BlogMetadata blog = AccountManager.getBlog(blogUrl);
        blog.setLoggedIn(isLoggedIn);
        AccountManager.addOrUpdateBlog(blog);
    }

}
