package actions;

import com.intellij.ide.DataManager;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.squareup.okhttp.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

public class BuildRelease extends AnAction {

    OkHttpClient client = new OkHttpClient();
    Process process = null;
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String command  = "git branch";
        String branchPath = runCommand(command);
        String branch = branchPath.split(" ")[1];
        String comment = "release";
        doGetRequest("http://android-jenkins.urbanclap.com:8080/job/service-market-customer-android-app/build", branch, comment);

        NotificationGroup noti = new NotificationGroup("prodDebugBuild", NotificationDisplayType.BALLOON, true);
        NotificationAction action = NotificationAction.createSimple("Slack", () ->{
            Desktop desktop = java.awt.Desktop.getDesktop();
            URI oURL = null;
            try {
                oURL = new URI("https://app.slack.com/client/T034MTGTM/CFD5QKJE9");
                desktop.browse(oURL);
            } catch (URISyntaxException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        noti.createNotification("Build",
                "ProdDebug build triggered on Jenkins: Check Slack",
                NotificationType.INFORMATION,
                null
        ).addAction(action).notify(e.getProject());
        process.destroy();
    }

    private String doGetRequest(String url, String branch, String comment) {
        String body = "{\"parameter\": [{\"name\":\"BRANCH\", \"value\":\"" + branch+ "\"}, {\"name\":\"PROJECT\", \"value\":\"service-market-customer-android-app\"}, {\"name\":\"COMMENT\", \"value\":\"#"+ comment +"\"}]}";
        RequestBody requestBody = new MultipartBuilder().addFormDataPart("json", body).build();
        Request request = new Request.Builder()
                .header("Jenkins-Crumb", "da0c9cfe21dcbd55606e4ce605277fdc")
                .header("Content-type","application/x-www-form-urlencoded")
                .header("Authorization", "Basic dml2ZWtzaW5naDoxMjNAdWNsYXA=")
                .post(requestBody)
                .url(url)
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.body().toString();
    }

    private String runCommand(String command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true);
        DataContext dataContext = DataManager.getInstance().getDataContext();
        Project project = (Project) dataContext.getData(DataConstants.PROJECT);
        processBuilder.directory(new File(project.getBasePath()));

        try {
            process = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getBranch(new BufferedReader(new InputStreamReader(process.getInputStream())));
    }

    private String getBranch(BufferedReader bufferedReader) {
        String line  = "";
        while (true) {
            try {
                line = bufferedReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (line == null) {
                return "stage";
            }
            else if (line.startsWith("*"))
                return line;
        }
    }
}

