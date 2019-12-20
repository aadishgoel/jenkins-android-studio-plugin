package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class BuildCustomBranch extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        MyForm buildBranch = new MyForm(e);
        buildBranch.setVisible(true);
    }
}
