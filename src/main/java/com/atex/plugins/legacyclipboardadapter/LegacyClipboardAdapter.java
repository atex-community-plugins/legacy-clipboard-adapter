package com.atex.plugins.legacyclipboardadapter;

import com.atex.onecms.clipboard.OneCMSClipBean;
import com.atex.onecms.clipboard.OneCMSClipboardBean;
import com.atex.onecms.clipboard.OneCMSClipboardPolicy;
import com.atex.onecms.content.ContentId;
import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.ContentResult;
import com.atex.onecms.content.ContentVersionId;
import com.atex.onecms.content.ContentWrite;
import com.atex.onecms.content.ContentWriteBuilder;
import com.atex.onecms.content.RepositoryClient;
import com.atex.onecms.content.SetAliasOperation;
import com.atex.onecms.content.Subject;
import com.atex.onecms.content.SubjectUtil;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CmClient;
import com.polopoly.common.logging.LogUtil;

import com.polopoly.pear.impl.ApplicationException;
import com.polopoly.pear.impl.InternalApplicationUtil;
import com.polopoly.tools.publicapi.annotations.PublicApi;
import com.polopoly.user.server.Caller;
import com.polopoly.user.server.jsp.ServletUserFactorySettings;
import com.polopoly.user.server.jsp.UserFactory;

import org.apache.commons.lang.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

@PublicApi
public class LegacyClipboardAdapter implements Filter{

    protected static final String CLASS = LegacyClipboardAdapter.class.getName();

    protected UserFactory localUserFactory;

    private static final String CLIPBOARD_EXT_ID_PREFIX = "atex.onecms.Clipboard-";

    private static final Logger LOG = LogUtil.getLog(CLASS);

    private static final String POLICY_DELEGATION_ID = "policy";

    private CmClient _cmClient;
    private RepositoryClient repositoryClient;
    private ContentManager contentManager;

    @Override
    public void destroy()
    {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException
    {
        HttpServletRequest  req  = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;

        String contentId = getValueFromCookies(req, "polopoly.clipboard");
        String oldContentId = getValueFromCookies(req, "oldClipboardId");

        if(StringUtils.isEmpty(contentId)){
            return;
        }

        Caller caller = null;

        try {
            Object[] userAndCaller = localUserFactory.getUserAndCallerIfPresent(req, resp);

            if (userAndCaller[1] != null) {
                caller = (Caller) userAndCaller[1];
            }

        } catch(Exception e){
            LOG.log(Level.WARNING, "Encountered exception while retrieving userId: " , e);
        }

        if(caller == null){
            LOG.log(Level.WARNING,"Could not resolve the caller");
            return;
        }

        if(oldContentId == null || !contentId.equalsIgnoreCase(oldContentId)){
            updateClipboard(contentId,caller);
            addUpdateCookie(contentId, resp, oldContentId);
        }else{
           LOG.log(Level.FINE,"The item in the clipboard has not changed. Caller: " + caller + " content id: " + contentId);
        }
    }

    private void addUpdateCookie(String contentId, HttpServletResponse response, String oldContentId){

        Cookie cookie;
        if (contentId != null && oldContentId != null && !contentId.equalsIgnoreCase(oldContentId)) {
            cookie = new Cookie("oldClipboardId", "" + contentId);
            cookie.setMaxAge(-1);
            cookie.setPath("/");
            response.addCookie(cookie);
            LOG.info("OldClipboardId cookie has been updated. New cookie value for oldClipboardId: " + cookie.getValue());
        }else{
            LOG.info("OldClipboardId cookie has not been updated.");
        }
    }

    private String getContentId(String value, String field){

        value = value.replaceAll("%3D", "=");
        value = value.replaceAll("%26", "&");

        StringTokenizer st = new StringTokenizer(value, "&");
        String result = null;
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            String[] arr = s.split("=");

            if (arr.length > 1) {

                if (arr[0].contains(field)) {
                    result = arr[1].trim();
                }
            }
        }
        return result;
    }

    private String getValueFromCookies(HttpServletRequest req, String cookie){
        Cookie[] cookies = req.getCookies();
        for(int i=0; i < cookies.length; i++) {
            if (cookies[i].getName().equalsIgnoreCase(cookie)) {
                String value = cookies[i].getValue();

                if(cookie.equalsIgnoreCase("polopoly.clipboard")){
                    return getContentId(value, "contentId");
                }else{
                    return value.trim();
                }
            }
        }
        return null;
    }

    private void updateClipboard(String id, Caller caller){

        try {

            String clipExtId = CLIPBOARD_EXT_ID_PREFIX + caller.getUserId().getPrincipalIdString();

            Subject subject = SubjectUtil.fromCaller(caller);

            if(subject == null){
                LOG.info("Could not resolve the subject for caller: " + caller);
                return;
            }

            ContentVersionId clipboardBeanId = contentManager.resolve(clipExtId, subject);

            ContentId contentId = new ContentId(POLICY_DELEGATION_ID,id);

            ContentVersionId contentVersionId = contentManager.resolve(contentId, null, subject);

            if (contentVersionId != null) {

                ContentResult contentResult = contentManager.get(contentVersionId, null, subject);
                if (!contentResult.getStatus().isSuccess()) {
                    throw new CMException("Couldn't get the content for: " + contentVersionId.toString() + ". Failed with status: " + contentResult.getStatus());
                }

                OneCMSClipBean clip = new OneCMSClipBean();

                clip.setId(contentVersionId.getContentId());

                clip.setType(contentResult.getContent().getContentDataType());

                List<OneCMSClipBean> list = new ArrayList<>();
                list.add(clip);

                OneCMSClipboardBean clipBean = new OneCMSClipboardBean();
                clipBean.setContent(list);

                if (clipboardBeanId != null) {

                    ContentWrite<OneCMSClipboardBean> contentWrite = new ContentWriteBuilder<OneCMSClipboardBean>()
                            .mainAspectData(clipBean).origin(clipboardBeanId)
                            .buildCreate();


                    contentManager.update(clipboardBeanId.getContentId(), contentWrite, subject);

                } else {
                    ContentWrite<OneCMSClipboardBean> contentWrite = new ContentWriteBuilder<OneCMSClipboardBean>()
                            .mainAspectData(clipBean).operations().
                                    operation(new SetAliasOperation(SetAliasOperation.EXTERNAL_ID, clipExtId))
                            .buildCreate();


                    ContentResult<OneCMSClipboardPolicy> result = contentManager.create(contentWrite, subject);
                    if (!result.getStatus().isSuccess()) {
                        throw new CMException("Failed to create OneCMSClipboardBean for subject: " +  subject + ". Failed with status: " + result.getStatus());
                    }


                }
            }else{
                LOG.info("Could not resolve ContentVersionId for ContentId: " + contentId);
            }

        } catch (Exception e) {
            LOG.log(Level.WARNING, "An error occured while attempting to copy legacy content:", e);
        }

    }


    @Override
    public void init(FilterConfig filterConfig)
            throws ServletException {
        try {
            repositoryClient = (RepositoryClient)
                    InternalApplicationUtil.getApplication(filterConfig.getServletContext()).getApplicationComponent(RepositoryClient.DEFAULT_COMPOUND_NAME);

        } catch (Exception e) {
            LOG.log(Level.WARNING,"An error occured during initialization of repository client:",e);
        }

        try{
            contentManager = repositoryClient.getContentManager();
        }catch (Exception e){
            LOG.log(Level.WARNING,"An error occured during initialization of content manager:",e);
        }

        try {
            _cmClient = InternalApplicationUtil.getCmClient(filterConfig.getServletContext());

            localUserFactory = new UserFactory(
                    new ServletUserFactorySettings(filterConfig),
                    _cmClient);
        } catch (ApplicationException e) {
            localUserFactory = UserFactory.getInstance();
            LOG.log(Level.WARNING,"An error occured during initialization of cmClient:",e);
        }
    }
}