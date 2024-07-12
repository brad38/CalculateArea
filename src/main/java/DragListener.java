
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class DragListener implements DropTargetListener {

    BufferedImage storeImage = null;
    private File droppedFile;
    JLabel imageLabel = new JLabel();
    // JLabel pathLabel = new JLabel();

    public DragListener(JLabel image) {
        imageLabel = image;
        //pathLabel = path;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
    }

    @Override
    public void drop(DropTargetDropEvent ev) {
        ev.acceptDrop(DnDConstants.ACTION_COPY);
        Transferable t = ev.getTransferable();
        DataFlavor[] df = t.getTransferDataFlavors();
        for (DataFlavor f : df) {
            try {
                if (f.isFlavorJavaFileListType()) {
                    List<File> files = (List<File>) t.getTransferData(f);
                    for (File file : files) {
                        droppedFile = file;  // Armazena o arquivo arrastado
                        displayImage(file.getPath());
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }

    public File getDroppedFile() {
        return droppedFile;
    }

    private void displayImage(String path) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(path));
            storeImage = img;
        } catch (Exception e) {
            System.out.println(e);
        }
        
        ImageIcon icon = new ImageIcon(img);
        imageLabel.setIcon(icon);
        imageLabel.setText("");
        
//pathLabel.setText(path);
    }

    public BufferedImage getStoredImage() {
        return storeImage;
    }
}
