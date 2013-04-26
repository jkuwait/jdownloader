package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.contextmenumanager.gui.ManagerFrame;

public class MenuManagerAction extends AppAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public MenuManagerAction(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setName(_GUI._.MenuManagerAction_MenuManagerAction());
        setIconKey("menu");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                ManagerFrame.show();
            }
        };

    }

}
