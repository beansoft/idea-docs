package org.jetbrains.tutorials.editor.basics;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.*;
import com.intellij.openapi.project.Project;

/**
 * @author Anna Bulenkova
 */
public class EditorIllustration extends AnAction {

  static {
    final EditorActionManager actionManager = EditorActionManager.getInstance();
    final TypedAction typedAction = actionManager.getTypedAction();
    typedAction.setupHandler(new MyTypedHandler());
  }

  @Override
  public void actionPerformed(final AnActionEvent e) {
    //Get all the required data from data keys
    final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
    final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    //Access document, caret, and selection
    final Document document = editor.getDocument();
    final SelectionModel selectionModel = editor.getSelectionModel();

    final int start = selectionModel.getSelectionStart();
    final int end = selectionModel.getSelectionEnd();
    //Making the replacement
    WriteCommandAction.runWriteCommandAction(project, () ->
        document.replaceString(start, end, "Replacement")
    );
    selectionModel.removeSelection();
  }

  @Override
  public void update(final AnActionEvent e) {
    //Get required data keys
    final Project project = e.getProject();
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    //Set visibility only in case of existing project and editor and if some text in the editor is selected
    e.getPresentation().setVisible((project != null && editor != null && editor.getSelectionModel().hasSelection()));
  }
}
