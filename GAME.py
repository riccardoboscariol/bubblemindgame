import streamlit as st
import numpy as np
import pygame
import time
import threading
from collections import Counter
from io import BytesIO
from PIL import Image

# Costanti
COLORS = ['#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF']
ENTROPY_THRESHOLD = 0.02
BOMB_THRESHOLD = 0.005
THEORETICAL_MAX_ENTROPY = 1

# Inizializzazione di Pygame
pygame.init()
screen_width, screen_height = 400, 600
screen = pygame.Surface((screen_width, screen_height))
clock = pygame.time.Clock()

# Stati del Gioco
bubbles = []
cannon_angle = 0
shot_bubble = None
next_bubble_color = COLORS[0]
entropy = 1
score = 0
game_over = False

# Funzione per calcolare l'entropia di Shannon
def calculate_entropy(bits):
    count = Counter(bits)
    total_bits = len(bits)
    entropy = -sum((freq / total_bits) * np.log2(freq / total_bits) for freq in count.values())
    return entropy / np.log2(2)

# Funzione per aggiornare l'entropia
def update_entropy():
    global entropy
    bits = np.random.randint(0, 2, 200)
    entropy = calculate_entropy(bits)

# Funzione per inizializzare le bolle
def initialize_bubbles():
    global bubbles
    bubbles = []
    cols = 8  # numero di colonne basato su screen_width e dimensione delle bolle
    bubble_radius = screen_width * 0.03
    for row in range(4):
        for col in range(cols):
            x = col * screen_width * 0.06 + screen_width * 0.03
            y = row * screen_width * 0.06 + screen_width * 0.03
            color = np.random.choice(COLORS)
            bubbles.append({'x': x, 'y': y, 'color': color})

# Funzione per disegnare lo sfondo
def draw_background(ctx):
    gradient = pygame.Surface((screen_width, screen_height))
    for i in range(100):
        pygame.draw.circle(gradient, (255, 255, 255), 
                           (np.random.randint(0, screen_width), np.random.randint(0, screen_height)), 
                           np.random.randint(1, 3), 0)
    ctx.blit(gradient, (0, 0))

# Funzione per disegnare le bolle
def draw_bubbles(ctx):
    bubble_radius = int(screen_width * 0.03)
    for bubble in bubbles:
        pygame.draw.circle(ctx, pygame.Color(bubble['color']), (int(bubble['x']), int(bubble['y'])), bubble_radius)
        pygame.draw.circle(ctx, pygame.Color('white'), (int(bubble['x']), int(bubble['y'])), bubble_radius, 1)

# Funzione per sparare una bolla
def shoot_bubble():
    global shot_bubble, next_bubble_color
    color = 'black' if entropy < BOMB_THRESHOLD else next_bubble_color
    shot_bubble = {
        'x': screen_width / 2,
        'y': screen_height - screen_width * 0.03,
        'color': color,
        'vx': np.sin(cannon_angle) * screen_width * 0.0125,
        'vy': -np.cos(cannon_angle) * screen_width * 0.0125
    }
    next_bubble_color = np.random.choice(COLORS)

# Funzione per muovere le bolle sparate
def move_bubble():
    global shot_bubble
    if shot_bubble:
        shot_bubble['x'] += shot_bubble['vx']
        shot_bubble['y'] += shot_bubble['vy']
        if shot_bubble['x'] < 0 or shot_bubble['x'] > screen_width or shot_bubble['y'] < 0:
            shot_bubble = None

# Funzione per controllare la collisione
def check_collision():
    global shot_bubble, bubbles, score
    if not shot_bubble:
        return
    bubble_radius = int(screen_width * 0.03)
    for bubble in bubbles:
        dx = shot_bubble['x'] - bubble['x']
        dy = shot_bubble['y'] - bubble['y']
        distance = np.sqrt(dx**2 + dy**2)
        if distance < bubble_radius * 2:
            bubbles.append({'x': shot_bubble['x'], 'y': shot_bubble['y'], 'color': shot_bubble['color']})
            shot_bubble = None
            score += 1
            break

# Funzione per aggiornare il gioco
def game_loop():
    global game_over
    initialize_bubbles()
    while not game_over:
        screen.fill((0, 0, 0))
        draw_background(screen)
        draw_bubbles(screen)
        if shot_bubble:
            move_bubble()
            check_collision()
        else:
            entropy_decrease = THEORETICAL_MAX_ENTROPY - entropy
            if entropy_decrease > ENTROPY_THRESHOLD:
                shoot_bubble()
        
        # Disegna il contenuto su Streamlit
        img = Image.frombytes('RGB', (screen_width, screen_height), pygame.image.tostring(screen, 'RGB'))
        st.image(img)

        pygame.display.flip()
        clock.tick(60)
        time.sleep(0.05)

# Streamlit UI
st.title("Mind Bubble Shooter")

if st.button("Start Game"):
    game_over = False
    threading.Thread(target=game_loop).start()

st.write(f"Score: {score}")
st.write(f"Entropy: {entropy:.4f}")
