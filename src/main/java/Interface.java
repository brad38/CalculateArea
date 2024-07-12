
//import org.opencv.core.Point;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
//import java.awt.Point;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import javax.swing.Icon;
//import javax.imageio.ImageIO;
//import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

public class Interface extends javax.swing.JFrame {

    public Interface() {
        initComponents();
        connectToDragDrop();
        jPanel1.requestFocusInWindow(); //Apenas solicita o foco ao "jPanel1"
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Inicializar OpenCV
    }
    //Variaveis
    private BufferedImage Original;
    private Mat hierarchy;
    private Mat binary;
    private Mat gray;
    private Mat drawing;
    private Mat matImage;
    private BufferedImage image;
    private double AreaValue;
    private Dimension ImageRes;
    private static boolean isDarkTheme = true;
    private String resolutionString;

    private void calculateArea() {
        if (image == null) {
            JOptionPane.showMessageDialog(this, "Por favor, carregue uma imagem primeiro.");
            return;
        }

        // Armazena a imagem original
        Original = image;

        // Armazena a resolução da imagem
        ImageRes = new Dimension(image.getWidth(), image.getHeight());

        // Formatar a resolução como uma string
        resolutionString = String.format("%d x %d", ImageRes.width, ImageRes.height);

        // Converter BufferedImage para Mat
        matImage = bufferedImageToMat(image);
        if (matImage.empty()) {
            JOptionPane.showMessageDialog(this, "Erro ao converter imagem.");
            return;
        }

        // Converter para escala de cinza
        gray = new Mat();
        Imgproc.cvtColor(matImage, gray, Imgproc.COLOR_BGR2GRAY);

        // Pré-processamento aplicando um filtro de desfoque, pra deixar os contornos mais polidos.
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0); // Desfoque Gaussiano

        // Limiarização
        binary = new Mat();
        Imgproc.threshold(blurred, binary, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        // Encontrar contornos 
        java.util.List<MatOfPoint> contours = new java.util.ArrayList<>();
        hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Só uma condição pra dar erro se nenhum contorno for encontrado.
        if (contours.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum contorno encontrado na imagem.");
            return;
        }

        // Cria uma matriz vazia com as mesmas dimensões da imagem e preenchida com zeros para desenhar os contornos.
        drawing = Mat.zeros(matImage.size(), CvType.CV_8UC3);

        // Desenhar os contornos na matriz vazia (drawing)
        Imgproc.drawContours(drawing, contours, -1, new Scalar(0, 255, 0), 2);

        // Calcular a área dos contornos
        double area = 0;
        for (MatOfPoint contour : contours) {
            area += Imgproc.contourArea(contour);
        }

        AreaValue = area;

        // Exibir a área calculada e a imagem com contornos
        //JOptionPane.showMessageDialog(this, "Área calculada: " + AreaValue);
        //showImage(drawing, "Contornos Detectados"); // Exibir a imagem 'drawing'
    }

    //Metodo pra exibir a imagem com filtro aplicados
    private void showImage(Mat mat, String title) {
        BufferedImage img = matToBufferedImage(mat);

        // Redimensionar a imagem
        int width = IconCalculate.getWidth();
        int height = IconCalculate.getHeight();
        Image scaledImage = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        // Criar um novo ImageIcon com a imagem redimensionada
        ImageIcon icon = new ImageIcon(scaledImage);

        // Definir o ícone na JLabel
        IconCalculate.setIcon(icon);
    }

    // Método para converter BufferedImage para Mat
    private Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    //Metodo pra conectar a class responsavel pelo DragAndDrop 
    private void connectToDragDrop() {
        DragListener d = new DragListener(IconLabel);
        new DropTarget(this, d);

        new Thread(() -> {
            try {
                // Aguardar o arquivo ser arrastado
                while (d.getDroppedFile() == null) {
                    Thread.sleep(100); // Pequeno delay para evitar busy-waiting
                }

                File file = d.getDroppedFile();

                // Usar OpenCV para carregar a imagem
                Mat mat = Imgcodecs.imread(file.getAbsolutePath());
                if (mat.empty()) {
                    throw new Exception("Erro ao carregar imagem");
                }

                // Converter Mat para BufferedImage
                image = matToBufferedImage(mat);

                // Variaveis que armazenam o local e o tammanho da label que ficará a imagem
                int Scalx = IconLabel.getSize().width - ((IconLabel.getSize().width * 20) / 100);
                int Scaly = IconLabel.getSize().height;

                // Setando o tamanho da imagem para caber na label
                Image scaledImage = image.getScaledInstance(Scalx, Scaly, Image.SCALE_SMOOTH);
                IconLabel.setIcon(new ImageIcon(scaledImage));
                pack();
            } catch (Exception ex) {
                IconLabel.setIcon(null); // Se o metodo der errado ele tira a foto da label, pra impedir que nada seja calculado
                JOptionPane.showMessageDialog(this, "Erro ao carregar imagem: " + ex.getMessage());
                IconLabel.setText("Clique em \"Carregar\" ou arraste uma imagem aqui");
            }

            if (image != null) { // se uma imagem for carregada, a mensagem da label é retirada
                IconLabel.setText("");
            }
        }).start();
    }

    //Metodo pra carregar a imagem
    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imagens", "jpg", "png", "bmp", "gif"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();

                // Usar OpenCV para carregar a imagem
                Mat mat = Imgcodecs.imread(file.getAbsolutePath());
                if (mat.empty()) {
                    throw new Exception("Erro ao carregar imagem");
                }

                // Converter Mat para BufferedImage
                image = matToBufferedImage(mat);

                // Variaveis que armazenam o local e o tammanho da label que ficará a imagem
                int Scalx = IconLabel.getSize().width - ((IconLabel.getSize().width * 20) / 100);
                int Scaly = IconLabel.getSize().height;

                // Setando o tamanho da imagem para caber na label
                Image scaledImage = image.getScaledInstance(Scalx, Scaly, image.SCALE_SMOOTH);
                IconLabel.setIcon(new ImageIcon(scaledImage));
                pack();
            } catch (Exception ex) {
                IconLabel.setIcon(null); // Se o metodo der errado ele tira a foto da label, pra impedir que nada seja calculado
                JOptionPane.showMessageDialog(this, "Erro ao carregar imagem: " + ex.getMessage());
            }
            if (image != null) { // se uma imagem for carregada, a mensagem da label é retirada
                IconLabel.setText("");
            }
        }
    }

// Método para converter Mat para BufferedImage
    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        QuemSomos = new javax.swing.JDialog();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        GojoLabel = new javax.swing.JLabel();
        Funcionamento = new javax.swing.JDialog();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        CalcularArea = new javax.swing.JDialog();
        jPanel3 = new javax.swing.JPanel();
        IconCalculate = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        FigureArea = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        PanelMenu1 = new javax.swing.JPanel();
        CarregarBTN1 = new javax.swing.JButton();
        GrayFilter = new javax.swing.JButton();
        BinaryFilter = new javax.swing.JButton();
        ContourFilter = new javax.swing.JButton();
        StandartFilter = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        PanelMenu = new javax.swing.JPanel();
        CarregarBTN = new javax.swing.JButton();
        CalcularBTN = new javax.swing.JButton();
        IconLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();

        QuemSomos.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        QuemSomos.setTitle("Quem somos?");
        QuemSomos.setAlwaysOnTop(true);
        QuemSomos.setLocation(new java.awt.Point(0, 0));
        QuemSomos.setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        QuemSomos.setUndecorated(true);
        QuemSomos.setSize(new java.awt.Dimension(800, 600));

        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Monospaced", 0, 18)); // NOI18N
        jTextArea1.setRows(5);
        jTextArea1.setText("Eu não nasci gay\nA culpa é do meu pai\nQue contratou um tal de Wilson\nPra ser capataz\nEu vi o bofe tomar banho\nE o tamanho da sua mala\nEra demais\nAlém de linda, era demais\n\nEu virei gay\nE me assumi\nA arte da pederastia\nE pude um dia, então sorrir\nPedi o Wilson em casamento\nE o jumento aceitou\nA Lua de mel foi no Egito\nEu fui pra cama e dei um grito\n\nE disse: Hey\nVai devagar, amor\nNão vai com força, ainda sou moça\nE não quero sentir dor\nMe trate como uma menina\nVaselina, por favor\nParecia Rambo\nCom a sua bazuca na minha nuca\n\nE disse: Vai\nAi, Wilson, vai\nEsfrega a mala na minha cara\nVai pra frente e vai pra trás\nPra malona eu dei um grito\nEntalei, quase eu vomito\nAi, Wilson, vai\nAi, Wilson, vai\nHey, hey\n\nAi, Wilson, vai\nAi, Wilson, vai\nHey, hey\n\nEu não nasci gay\nA culpa é do meu pai\nQue contratou um tal de Wilson\nPra ser o capataz\nEu vi o bofe tomar banho\nE o tamanho da sua mala\nEra demais\nAlém de linda, era demais\n\nEu virei gay\nMe assumi\nA arte da pederastia\nE pude um dia, então sorrir\nPedi o Wilson em casamento\nE o jumento aceitou\nA Lua de mel foi no Egito\nEu fui pra cama e dei um grito\n\nE disse: Hey\nVai devagar, amor\nNão vai com força, ainda sou moça\nE não quero sentir dor\nMe trate como uma menina\nVaselina, por favor\nParecia Rambo\nCom a sua bazuca na minha nuca\n\nE disse: Vai\nAi, Wilson, vai\nEsfrega a mala na minha cara\nVai pra frente e vai pra trás\nPra malona eu dei um grito\nEntalei, quase eu vomito\nAi, Wilson, Vai\nAi, Wilson, Vai\nHey, hou\n\nEu virei gay\nMe assumi\nA arte da pederastia\nE pude um dia, então sorrir\nPedi o Wilson em casamento\nE o jumento aceitou\nA Lua de mel foi no Egito\nEu fui pra cama e dei um grito\n\nE disse: Hey\nVai devagar, amor\nNão vai com força, ainda sou moça\nE não quero sentir dor\nMe trate como uma menina\nVaselina, por favor\nParecia Rambo\nCom a sua bazuca na minha nuca\n\nE disse: Vai\nAi, Wilson, vai\nEsfrega a mala na minha cara\nVai pra frente e vai pra trás\nPra malona eu dei um grito\nEntalei, quase eu vomito\nAi, Wilson, vai\nAi, Wilson, vai\nHey, hey");
        jScrollPane1.setViewportView(jTextArea1);

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel2.setText("Quem somos?");

        jButton1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jButton1.setText("Voltar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout QuemSomosLayout = new javax.swing.GroupLayout(QuemSomos.getContentPane());
        QuemSomos.getContentPane().setLayout(QuemSomosLayout);
        QuemSomosLayout.setHorizontalGroup(
            QuemSomosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(QuemSomosLayout.createSequentialGroup()
                .addGap(57, 57, 57)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(QuemSomosLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(QuemSomosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(QuemSomosLayout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE)
                        .addGap(267, 267, 267)
                        .addComponent(GojoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(198, 198, 198))
                    .addComponent(jScrollPane1))
                .addGap(31, 31, 31))
        );
        QuemSomosLayout.setVerticalGroup(
            QuemSomosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, QuemSomosLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(QuemSomosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(GojoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
                .addGap(10, 10, 10)
                .addComponent(jButton1)
                .addContainerGap())
        );

        Funcionamento.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        Funcionamento.setTitle("FUNCIONAMENTO");
        Funcionamento.setAlwaysOnTop(true);
        Funcionamento.setLocation(new java.awt.Point(0, 0));
        Funcionamento.setModal(true);
        Funcionamento.setUndecorated(true);
        Funcionamento.getContentPane().setLayout(new javax.swing.BoxLayout(Funcionamento.getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        jPanel2.setPreferredSize(new java.awt.Dimension(800, 600));

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Tamo trabalhando nisso ainda, bb. Te acalma!");
        jLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jButton3.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jButton3.setText("Voltar");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(132, Short.MAX_VALUE)
                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(121, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(671, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(278, Short.MAX_VALUE)
                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 236, Short.MAX_VALUE)
                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21))
        );

        Funcionamento.getContentPane().add(jPanel2);

        CalcularArea.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        CalcularArea.setTitle("Calcular Área");
        CalcularArea.setAlwaysOnTop(true);
        CalcularArea.setModal(true);
        CalcularArea.setUndecorated(true);
        CalcularArea.getContentPane().setLayout(new java.awt.BorderLayout());

        jPanel3.setPreferredSize(new java.awt.Dimension(800, 600));

        IconCalculate.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        IconCalculate.setBorder(javax.swing.BorderFactory.createLineBorder(null));
        IconCalculate.setIcon(null);

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel7.setText("Área da figura (p²)");

        FigureArea.setEditable(false);

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel8.setText("Resolução da imagem");

        jTextField2.setEditable(false);

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel9.setText("Filtro de visualização:");

        PanelMenu1.setBackground(new java.awt.Color(34, 34, 34));

        CarregarBTN1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        CarregarBTN1.setText("Voltar");
        CarregarBTN1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                CarregarBTN1MousePressed(evt);
            }
        });
        CarregarBTN1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CarregarBTN1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout PanelMenu1Layout = new javax.swing.GroupLayout(PanelMenu1);
        PanelMenu1.setLayout(PanelMenu1Layout);
        PanelMenu1Layout.setHorizontalGroup(
            PanelMenu1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelMenu1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(CarregarBTN1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        PanelMenu1Layout.setVerticalGroup(
            PanelMenu1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelMenu1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(CarregarBTN1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        GrayFilter.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        GrayFilter.setText("Tom de cinza");
        GrayFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GrayFilterActionPerformed(evt);
            }
        });

        BinaryFilter.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        BinaryFilter.setText("Binarizado");
        BinaryFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BinaryFilterActionPerformed(evt);
            }
        });

        ContourFilter.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        ContourFilter.setText("Contornos");
        ContourFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContourFilterActionPerformed(evt);
            }
        });

        StandartFilter.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        StandartFilter.setText("Padrão");
        StandartFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StandartFilterActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PanelMenu1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(IconCalculate, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel9))
                        .addGap(68, 68, 68)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(FigureArea)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(45, 45, 45)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jTextField2)
                                .addGap(34, 34, 34))
                            .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(74, 74, 74))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(BinaryFilter, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(GrayFilter, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(ContourFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(StandartFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(IconCalculate, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(GrayFilter)
                            .addComponent(StandartFilter))
                        .addGap(28, 28, 28)
                        .addComponent(BinaryFilter)
                        .addGap(26, 26, 26)
                        .addComponent(ContourFilter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(68, 68, 68)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(FigureArea)
                            .addComponent(jTextField2))
                        .addGap(433, 433, 433)))
                .addComponent(PanelMenu1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        CalcularArea.getContentPane().add(jPanel3, java.awt.BorderLayout.CENTER);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Projeto Integrador - II");
        setAlwaysOnTop(true);

        jPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel1MouseClicked(evt);
            }
        });

        PanelMenu.setBackground(new java.awt.Color(34, 34, 34));

        CarregarBTN.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        CarregarBTN.setText("Carregar");
        CarregarBTN.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                CarregarBTNMousePressed(evt);
            }
        });
        CarregarBTN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CarregarBTNActionPerformed(evt);
            }
        });

        CalcularBTN.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        CalcularBTN.setText("Calcular Área");
        CalcularBTN.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                CalcularBTNMousePressed(evt);
            }
        });
        CalcularBTN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CalcularBTNActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout PanelMenuLayout = new javax.swing.GroupLayout(PanelMenu);
        PanelMenu.setLayout(PanelMenuLayout);
        PanelMenuLayout.setHorizontalGroup(
            PanelMenuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelMenuLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(CarregarBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(CalcularBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        PanelMenuLayout.setVerticalGroup(
            PanelMenuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelMenuLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(PanelMenuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CarregarBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CalcularBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        IconLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        IconLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        IconLabel.setText("Clique em \"Carregar\" e selecione uma imagem dos seus arquivos");
        IconLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PanelMenu, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(IconLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
                .addGap(29, 29, 29))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(IconLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(PanelMenu, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jMenuBar1.setBackground(new java.awt.Color(102, 102, 102));
        jMenuBar1.setForeground(new java.awt.Color(255, 255, 255));
        jMenuBar1.setToolTipText("");
        jMenuBar1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        jMenu1.setText("Inicio");
        jMenu1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        jMenuItem1.setText("Mudar tema");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Sobre");
        jMenu2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        jMenuItem2.setText("Como funciona?");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuItem3.setText("Desenvolvedores");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem3);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jPanel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel1MouseClicked
        jPanel1.requestFocusInWindow();
    }//GEN-LAST:event_jPanel1MouseClicked

    private void CarregarBTNMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CarregarBTNMousePressed
        CarregarBTN.setFocusable(true); //Apenas solicita o foco para o botão clicado com o mouse
        CalcularBTN.setFocusable(false);
        loadImage();
    }//GEN-LAST:event_CarregarBTNMousePressed

    private void CalcularBTNMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CalcularBTNMousePressed
        CalcularBTN.setFocusable(true); //Apenas solicita o foco para o botão clicado com o mouse
        CarregarBTN.setFocusable(false);
    }//GEN-LAST:event_CalcularBTNMousePressed

    private void CarregarBTNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CarregarBTNActionPerformed
        CarregarImagem carregarImagem = new CarregarImagem();
        carregarImagem.setVisible(true);
    }//GEN-LAST:event_CarregarBTNActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // Aplicando temas
        try {
            if (isDarkTheme) {
                // Muda para o tema claro
                UIManager.setLookAndFeel(new FlatLightLaf()); //Setando o tema
                UIManager.put("MenuBar.borderColor", new java.awt.Color(200, 200, 200)); //Cor da linha abaixo da barra de menu
                PanelMenu.setBackground(new java.awt.Color(233, 233, 233)); // Cor do fundo do retangulo que fica os botões
                PanelMenu1.setBackground(new java.awt.Color(233, 233, 233)); // Cor do fundo do retangulo que fica os botões no menu de "Calcular Área"
                PanelMenu.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(212, 212, 212), 3)); // Cor da borda do retangulo que fica os botões
                PanelMenu1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(212, 212, 212), 3)); // Cor da borda do retangulo que fica os botões no menu de "Calcular Área"
                jMenuBar1.setForeground(new java.awt.Color(33, 33, 33)); //Cor das letras da menubar
            } else {
                // Muda para o tema escuro
                UIManager.setLookAndFeel(new FlatDarkPurpleIJTheme());
                UIManager.put("MenuBar.borderColor", Color.white);
                PanelMenu.setBackground(new java.awt.Color(33, 33, 33));
                PanelMenu1.setBackground(new java.awt.Color(33, 33, 33));
                PanelMenu.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 3));
                PanelMenu1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 3));
                jMenuBar1.setForeground(new java.awt.Color(255, 255, 255));
            }

            // Atualiza a aparência de todos os componentes
            SwingUtilities.updateComponentTreeUI(this);
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }

            // Alterna o estado do tema
            isDarkTheme = !isDarkTheme;
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void CalcularBTNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CalcularBTNActionPerformed
        if (image != null) {
            calculateArea();
            FigureArea.setText(String.valueOf(AreaValue));
            jTextField2.setText(resolutionString);

            //Variaveis que armazenam o local da tela que esta o "jPanel1"
            int newX = jPanel1.getLocationOnScreen().x;
            int newY = jPanel1.getLocationOnScreen().y - 24;

            //Variaveis que armazenam o tamanho da tela ocupado pelo "jPanel1"
            int Tamx = jPanel1.getSize().width + 1;
            int Tamy = jPanel1.getSize().height + 24;

            //Variaveis que armazenam o tamanho inicial da label que ficará a imagem no menu "Calcular Área"
            int Propx = IconLabel.getSize().width - ((IconLabel.getSize().width * 60) / 100);
            int Propy = IconLabel.getSize().height - ((IconLabel.getSize().height * 50) / 100);;

            //Redimensionando a imagem de acordo com o tamanho da label(label do menu "calcular Area")
            Image scaledImage2 = image.getScaledInstance(Propx, Propy, image.SCALE_SMOOTH);
            IconCalculate.setIcon(new ImageIcon(scaledImage2));

            //Alterando os paramêtros de inicialização do menu "Calcular Area"
            Dimension newPreferredSize = new Dimension(Tamx, Tamy);
            CalcularArea.setSize(newPreferredSize);
            CalcularArea.setLocation(newX, newY);
            CalcularArea.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Carregue uma imagem primeiro.", "Sem imagem", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_CalcularBTNActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        //Variaveis que armazenam o local da tela que esta o "jPanel1"
        int newX = jPanel1.getLocationOnScreen().x;
        int newY = jPanel1.getLocationOnScreen().y - 24;

        //Variaveis que armazenam o tamanho da tela ocupado pelo "jPanel1" 
        int Tamx = jPanel1.getSize().width + 1;
        int Tamy = jPanel1.getSize().height + 24;

        //Alterando os paramêtros de inicialização do menu "Quem somos"
        Dimension newPreferredSize = new Dimension(Tamx, Tamy);
        QuemSomos.setSize(newPreferredSize);
        QuemSomos.setLocation(newX, newY);
        QuemSomos.setVisible(true);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        QuemSomos.dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        //Variaveis que armazenam o local e tamanho na tela do "jPanel1"      
        int newX = jPanel1.getLocationOnScreen().x;
        int newY = jPanel1.getLocationOnScreen().y - 24;
        int Tamx = jPanel1.getSize().width + 1;
        int Tamy = jPanel1.getSize().height + 24;

        //Alterando os paramêtros de inicialização do menu "Funcionamento"    
        Dimension newPreferredSize = new Dimension(Tamx, Tamy);
        Funcionamento.setSize(newPreferredSize);
        Funcionamento.setLocation(newX, newY);
        Funcionamento.setVisible(true);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        Funcionamento.dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void StandartFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StandartFilterActionPerformed
        showImage(bufferedImageToMat(Original), "Imagem Original");
    }//GEN-LAST:event_StandartFilterActionPerformed

    private void ContourFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContourFilterActionPerformed
        showImage(drawing, "Contornos Detectados");
    }//GEN-LAST:event_ContourFilterActionPerformed

    private void BinaryFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BinaryFilterActionPerformed
        showImage(binary, "Imagem Binarizada");
    }//GEN-LAST:event_BinaryFilterActionPerformed

    private void GrayFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GrayFilterActionPerformed
        showImage(gray, "Imagem em Escala de Cinza"); // Metodo "showImage()" é chamado pra exibir a imagem em diferentes filtros.
    }//GEN-LAST:event_GrayFilterActionPerformed

    private void CarregarBTN1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CarregarBTN1ActionPerformed
        CalcularArea.dispose();
    }//GEN-LAST:event_CarregarBTN1ActionPerformed

    private void CarregarBTN1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CarregarBTN1MousePressed
        // TODO add your handling code here:
    }//GEN-LAST:event_CarregarBTN1MousePressed

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java455");
        try {

            // Mudando o look and feel para o tema FlatDarkPurpleIJTheme
            UIManager.setLookAndFeel(new FlatDarkPurpleIJTheme());
            UIManager.put("MenuBar.borderColor", Color.white);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Interface().setVisible(true);
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BinaryFilter;
    private javax.swing.JDialog CalcularArea;
    private javax.swing.JButton CalcularBTN;
    private javax.swing.JButton CarregarBTN;
    private javax.swing.JButton CarregarBTN1;
    private javax.swing.JButton ContourFilter;
    private javax.swing.JTextField FigureArea;
    private javax.swing.JDialog Funcionamento;
    private javax.swing.JLabel GojoLabel;
    private javax.swing.JButton GrayFilter;
    private javax.swing.JLabel IconCalculate;
    private javax.swing.JLabel IconLabel;
    private javax.swing.JPanel PanelMenu;
    private javax.swing.JPanel PanelMenu1;
    private javax.swing.JDialog QuemSomos;
    private javax.swing.JButton StandartFilter;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
