import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ImageAreaCalculator extends JFrame {
    private BufferedImage image;
    private JLabel imageLabel;
    private JButton loadButton;
    private JButton calculateButton;
    private JTextField resultField;
    //Inicializador e parâmetros da Janela Principal
    public ImageAreaCalculator() {
        setTitle("Calculadora de Área da Imagem");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        imageLabel = new JLabel(); 
        loadButton = new JButton("Carregar Imagem"); // Botão de Carregar imagem
        calculateButton = new JButton("Calcular Área"); // Botão de Calcular área
        resultField = new JTextField(20); // Tamanho do campo de texto do resultado
        resultField.setEditable(false);

        loadButton.addActionListener(new ActionListener() { // Função do botão de Carregar imagem
            @Override
            public void actionPerformed(ActionEvent e) {
                loadImage();
            }
        });

        calculateButton.addActionListener(new ActionListener() { // Função do botão de calcular área
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateArea();
            }
        });
        //Inicializador e parâmetros dos paineis dos botões
        JPanel panel = new JPanel();
        panel.add(loadButton);
        panel.add(calculateButton);
        panel.add(new JLabel("Área:"));
        panel.add(resultField);

        add(panel, BorderLayout.SOUTH);
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);
    }
    //Método de carregar imagem
    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imagens", "jpg", "png", "bmp", "gif"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                image = ImageIO.read(file);
                imageLabel.setIcon(new ImageIcon(image));
                pack();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao carregar imagem: " + ex.getMessage());
            }
        }
    }
    //Método de calcular área
    private void calculateArea() {
        if (image == null) {
            JOptionPane.showMessageDialog(this, "Por favor, carregue uma imagem primeiro.");
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        long area = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xff;
                if (alpha > 0) { 
                    area++;
                }
            }
        }

        resultField.setText(String.valueOf(area));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ImageAreaCalculator().setVisible(true);
            }
        });
    }
}
