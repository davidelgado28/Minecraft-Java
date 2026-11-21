# Minecraft-JavaSwing

Um protótipo funcional de um jogo de blocos 3D estilo **Minecraft Alpha**, desenvolvido inteiramente em **Java puro** utilizando apenas as bibliotecas nativas `Swing` e `AWT`, sem depender de motores externos ou OpenGL/LWJGL.

---

## Como Jogar

### 1. Pré-requisitos
* **Java Development Kit (JDK) 17** ou superior instalado na máquina.

### 2. Compilando e Executando
Abra o terminal na pasta onde o arquivo `MinecraftAlphaSwing.java` está salvo e execute os comandos abaixo:

```bash
javac MinecraftAlphaSwing.java
java MinecraftAlphaSwing
```

---

## Controles

Assim que a janela do jogo abrir, **clique no centro da tela** para capturar o cursor do mouse e ativar a câmera em primeira pessoa.

| Tecla / Botão | Ação |
| :--- | :--- |
| **Clique na Tela** | Captura o mouse para olhar em 360° |
| **`ESC`** | Libera o cursor do mouse |
| **`W` `A` `S` `D`** | Movimentar o personagem (Frente, Esquerda, Trás, Direita) |
| **`Espaço`** | Pular |
| **Mover o Mouse** | Olhar ao redor (cima, baixo, lados) |
| **Botão Esquerdo do Mouse** | Quebrar o bloco selecionado pela mira |
| **Botão Direito do Mouse** | Colocar um bloco na face selecionada |
| **Teclas `1` a `5`** | Alternar o tipo de bloco na mão |

---

## Blocos Disponíveis

Use as teclas numéricas para escolher o material de construção (o bloco ativo aparece no canto superior esquerdo da tela):

* **`1`** — **Pedregulho** (*Cobblestone*)
* **`2`** — **Grama** (*Grass*)
* **`3`** — **Terra** (*Dirt*)
* **`4`** — **Pedra** (*Stone*)
* **`5`** — **Madeira** (*Wood*)

---

## Recursos Implementados

* **Motor 3D via Software:** Renderização em tempo real feita diretamente na CPU através do algoritmo *3D DDA (Digital Differential Analyzer)*.
* **Geração Procedural de Mundo:** Mapa de `64x32x64` blocos com relevo ondulado, camadas de pedra/terra/grama e árvores geradas aleatoriamente.
* **Estética Clássica Alpha:** Texturas pixeladas `16x16` geradas por código, iluminação direcional por face e neblina (*fog*) no horizonte.
* **Física Básica:** Gravidade, pulo e detecção de colisão para impedir que o jogador atravesse paredes ou fique preso dentro de blocos recém-colocados.

---

## Dicas

* Se cair para fora dos limites do mapa, o jogo reposicionará o jogador automaticamente no topo do mundo.
