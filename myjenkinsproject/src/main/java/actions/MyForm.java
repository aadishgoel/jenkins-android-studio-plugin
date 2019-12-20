package actions;

import com.intellij.ide.DataManager;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.squareup.okhttp.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class MyForm extends JDialog{
    private JTextField gitBranchEditText;
    private JPanel contentPane;
    private JButton submit;
    private JLabel gitBranch;
    private JLabel buildType;
    private JButton cancel;
    private JRadioButton stageRadioButton;
    private JRadioButton prodDebugRadioButton;
    private JRadioButton releaseRadioButton;
    private JComboBox allBranches;
    private AnActionEvent anActionEvent;
    OkHttpClient client = new OkHttpClient();
    Process process = null;

    MyForm() {}

    MyForm(AnActionEvent actionEvent) {
        anActionEvent=actionEvent;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(submit);
        Editor editor = DataKeys.EDITOR.getData(actionEvent.getDataContext());
        int width=500,height=500;
        if(editor!=null) {
            if (editor.getComponent() != null) {
                width = editor.getComponent().getWidth();
                height = editor.getComponent().getHeight();
            }
        }
        submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSubmit();
            }
        });

        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

//        String command  = "git branch";
//        String branchPath = runCommand(command);

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        pack();
    }

    private void onSubmit() {
        String branch = gitBranchEditText.getText();
        String comment = null;
        if(stageRadioButton.isSelected())
            comment = "debug";
        else if(prodDebugRadioButton.isSelected())
            comment = "prodDebug";
        else if(releaseRadioButton.isSelected())
            comment = "release";

        if(comment != null && !comment.isEmpty()) {
            fireBuild(branch, comment);
        }
    }

    private void fireBuild(String branch, String comment) {
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
                comment + " build triggered on Jenkins: Check Slack",
                NotificationType.INFORMATION,
                null
        ).addAction(action).notify(anActionEvent.getProject());
        process.destroy();
        onCancel();
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

//    private String runCommand(String command) {
//        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
//        processBuilder.redirectErrorStream(true);
//        DataContext dataContext = DataManager.getInstance().getDataContext();
//        Project project = (Project) dataContext.getData(DataConstants.PROJECT);
//        processBuilder.directory(new File(project.getBasePath()));
//
//        try {
//            process = processBuilder.start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return getBranch(new BufferedReader(new InputStreamReader(process.getInputStream())));
//    }

    private String[] getBranch(BufferedReader bufferedReader) {
        String line  = "";
        String[] branchesArray = new String[256];
        int i = 0;
        while (true) {
            line = null;
            try {
                line = bufferedReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (line != null) {
                branchesArray[i++] = line;
            } else break;
        }
        return branchesArray;
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}