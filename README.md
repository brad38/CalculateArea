# 🧮 Calculadora de Área com OpenCV em Java

Este projeto é uma **calculadora de área** desenvolvida em **Java** com integração da biblioteca **OpenCV**, que permite processar imagens e calcular a área de objetos presentes nelas. Ideal para aplicações em visão computacional, análises de formas ou medições automáticas.

## 🚀 Funcionalidades

- Carregamento de imagem via interface gráfica (JFrame)
- Detecção de contornos utilizando OpenCV
- Cálculo da área de contornos em **pixels²**
- Exibição da resolução da imagem
- Conversão opcional da área de pixels² para **metros²** (usando escala)

## 🛠️ Tecnologias e Bibliotecas

- Java (JDK 8+)
- OpenCV (versão 4.x ou compatível)
- Swing (para interface gráfica)

## 🖼️ Exemplo de uso

1. O usuário carrega uma imagem com um objeto a ser medido.
2. O programa detecta os contornos.
3. A área de cada contorno é exibida em pixels².
4. (Opcional) Se o usuário fornecer a escala (ex: quantos pixels equivalem a 1 metro), o programa converte a área para metros².
   
## 📦 Como executar

1. **Clone o repositório**:
   ```bash
   git clone https://github.com/brad38/CalculateArea.git
