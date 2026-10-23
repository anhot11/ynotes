/**
 * yNotes — Script Principal
 * Funcionalidad: Tema claro/oscuro, Navegación móvil, Galería interactiva, FAQ y Scroll Reveal
 * 100% Sin dependencias externas ni telemetría
 */

document.addEventListener('DOMContentLoaded', () => {
  // 1. GESTIÓN DE TEMA (CLARO / OSCURO)
  initTheme();

  // 2. HEADER Y NAVEGACIÓN MÓVIL
  initNavigation();

  // 3. ANIMACIONES DE SCROLL REVEAL
  initScrollReveal();

  // 4. GALERÍA DE CAPTURAS INTERACTIVA
  initGallery();

  // 5. ACORDEÓN DE PREGUNTAS FRECUENTES (FAQ)
  initFAQ();
});

/**
 * Control del Modo Oscuro / Claro con persistencia en localStorage
 * y sincronización con las preferencias del sistema operativo.
 */
function initTheme() {
  const toggleBtn = document.getElementById('themeToggleBtn');
  const params = new URLSearchParams(window.location.search);
  const urlTheme = params.get('theme');
  const storedTheme = localStorage.getItem('ynotes_theme');
  const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)');

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    if (toggleBtn) {
      const isDark = theme === 'dark';
      toggleBtn.setAttribute('aria-label', isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
      toggleBtn.setAttribute('title', isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
    }
  }

  // Determinar tema inicial
  if (urlTheme === 'dark' || urlTheme === 'light') {
    applyTheme(urlTheme);
  } else if (storedTheme === 'dark' || storedTheme === 'light') {
    applyTheme(storedTheme);
  } else {
    applyTheme(systemPrefersDark.matches ? 'dark' : 'light');
  }

  // Escuchar botón de cambio manual
  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => {
      const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
      const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
      applyTheme(newTheme);
      localStorage.setItem('ynotes_theme', newTheme);
    });
  }

  // Escuchar cambios del sistema en tiempo real si el usuario no tiene preferencia fija
  systemPrefersDark.addEventListener('change', (e) => {
    if (!localStorage.getItem('ynotes_theme')) {
      applyTheme(e.matches ? 'dark' : 'light');
    }
  });
}

/**
 * Control del header con blur al hacer scroll y menú móvil responsive
 */
function initNavigation() {
  const header = document.querySelector('.site-header');
  const mobileToggle = document.getElementById('mobileMenuToggle');
  const navMenu = document.getElementById('navMenu');

  // Efecto scrolled en header
  window.addEventListener('scroll', () => {
    if (window.scrollY > 20) {
      header?.classList.add('scrolled');
    } else {
      header?.classList.remove('scrolled');
    }
  }, { passive: true });

  // Menú hamburguesa móvil
  if (mobileToggle && navMenu) {
    mobileToggle.addEventListener('click', () => {
      const isOpen = navMenu.classList.toggle('open');
      mobileToggle.setAttribute('aria-expanded', isOpen.toString());
    });

    // Cerrar menú al hacer clic en cualquier enlace
    navMenu.querySelectorAll('.nav-link').forEach(link => {
      link.addEventListener('click', () => {
        navMenu.classList.remove('open');
        mobileToggle.setAttribute('aria-expanded', 'false');
      });
    });

    // Cerrar al hacer clic fuera del menú
    document.addEventListener('click', (e) => {
      if (!navMenu.contains(e.target) && !mobileToggle.contains(e.target) && navMenu.classList.contains('open')) {
        navMenu.classList.remove('open');
        mobileToggle.setAttribute('aria-expanded', 'false');
      }
    });
  }
}

/**
 * Animaciones suaves al hacer scroll mediante IntersectionObserver
 */
function initScrollReveal() {
  document.documentElement.classList.add('js-loaded');
  const revealElements = document.querySelectorAll('.reveal');

  if ('IntersectionObserver' in window) {
    const revealObserver = new IntersectionObserver((entries, observer) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('revealed');
          observer.unobserve(entry.target);
        }
      });
    }, {
      root: null,
      threshold: 0.08,
      rootMargin: '0px 0px 50px 0px'
    });

    revealElements.forEach(el => revealObserver.observe(el));
  } else {
    // Fallback para navegadores antiguos
    revealElements.forEach(el => el.classList.add('revealed'));
  }
}

/**
 * Galería interactiva con información detallada de cada pantalla
 */
const galleryScreens = [
  {
    id: 'dashboard',
    title: 'Pantalla Principal AMOLED Glass (v1.0.20)',
    subtitle: 'Notas organizadas con máxima legibilidad, estética AMOLED pura y alto contraste.',
    src: 'assets/screenshots/ynotes-main-dashboard.webp',
    alt: 'Pantalla principal de yNotes con notas guardadas, buscador y botón flotante degradado',
    bullets: [
      'Diseño AMOLED puro con tarjetas tipo masonry y colores vivos personalizados.',
      'Buscador instantáneo por título, contenido o palabra clave con orden dinámico.',
      'Botón flotante con degradado suave para crear notas en un toque.',
      'Soporte de previsualización limpia con sintaxis Markdown oculta.'
    ]
  },
  {
    id: 'vault',
    title: 'Zona Segura & Bóveda AES-256',
    subtitle: 'El rincón impenetrable para tus notas más íntimas y aplicaciones confidenciales.',
    src: 'assets/screenshots/ynotes-secure-vault.webp',
    alt: 'Bóveda Segura de yNotes con cifrado AES-256-GCM y aplicaciones ocultas',
    bullets: [
      'Cifrado simétrico AES-256-GCM de alta seguridad con protección activa.',
      'Lanzador de aplicaciones confidenciales camufladas con carga instantánea.',
      'Botón dedicado "Nuevo secreto" con aislamiento criptográfico total.',
      'Copias de seguridad cifradas con clave zero-knowledge para máxima privacidad.'
    ]
  },
  {
    id: 'theme',
    title: 'Temas & Apariencia (Google Notes / Samsung)',
    subtitle: 'Adapta la interfaz visual a tu estilo con selector de diseño en tiempo real.',
    src: 'assets/screenshots/ynotes-theme-google.webp',
    alt: 'Pantalla principal con el estilo visual Google Notes y botón flotante redondeado',
    bullets: [
      'Estilo Google Notes con botón flotante clásico y navegación simplificada.',
      'Estilo Samsung ergonómico diseñado para uso cómodo a una sola mano.',
      'Estilo AMOLED Moderno por defecto con negros absolutos para ahorro de batería.',
      'Cambio instantáneo desde los ajustes sin tener que reiniciar la app.'
    ]
  },
  {
    id: 'safezone',
    title: 'Seguridad Avanzada & Modo Reversa',
    subtitle: 'Control total de autenticación biométrica y señuelos contra miradas curiosas.',
    src: 'assets/screenshots/ynotes-safezone-settings.webp',
    alt: 'Ajustes de Zona Segura con bloqueo de huella y opción de Modo Reversa',
    bullets: [
      'Bloqueo por huella dactilar respaldado por el hardware seguro de Android.',
      'Modo Reversa: transforma la vista principal en una lista señuelo si abres bajo coacción.',
      'Sonidos de la app configurables con síntesis SoundPool de baja latencia.',
      'Integración directa con formato Markdown y activación por nota.'
    ]
  },
  {
    id: 'settings',
    title: 'Ajustes Generales v1.0.20 (AMOLED Edition)',
    subtitle: 'Control total de la privacidad, sensaciones y personalización del sistema.',
    src: 'assets/screenshots/ynotes-settings-security.webp',
    alt: 'Pantalla de ajustes generales mostrando versión 1.0.20 AMOLED Edition y selector de apariencia',
    bullets: [
      'Versión oficial v1.0.20 (AMOLED Edition) con optimización de memoria RAM.',
      'Selector de Apariencia con 3 estilos visuales: Actual, Google y Samsung.',
      'Gestión de libros, categorías y notas fijadas en pantalla de inicio.',
      'Widgets interactivos para pantalla principal con soporte de notas públicas o secretas.'
    ]
  }
];

function initGallery() {
  const tabsContainer = document.getElementById('galleryTabs');
  const showcaseImg = document.getElementById('galleryImg');
  const showcaseTitle = document.getElementById('galleryTitle');
  const showcaseSub = document.getElementById('gallerySub');
  const showcaseBullets = document.getElementById('galleryBullets');
  const thumbsContainer = document.getElementById('galleryThumbs');

  if (!tabsContainer || !showcaseImg) return;

  function setScreen(index) {
    const screen = galleryScreens[index];
    if (!screen) return;

    // Actualizar tabs activas
    tabsContainer.querySelectorAll('.gallery-tab-btn').forEach((btn, i) => {
      btn.classList.toggle('active', i === index);
      btn.setAttribute('aria-selected', (i === index).toString());
    });

    // Actualizar miniaturas activas
    if (thumbsContainer) {
      thumbsContainer.querySelectorAll('.thumb-card').forEach((thumb, i) => {
        thumb.classList.toggle('active', i === index);
      });
    }

    // Efecto suave de transición
    showcaseImg.style.opacity = '0.3';
    showcaseImg.style.transform = 'scale(0.98)';

    setTimeout(() => {
      showcaseImg.src = screen.src;
      showcaseImg.alt = screen.alt;
      showcaseTitle.textContent = screen.title;
      showcaseSub.textContent = screen.subtitle;

      // Renderizar viñetas
      showcaseBullets.innerHTML = screen.bullets.map(b => `
        <li>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"></polyline></svg>
          <span>${b}</span>
        </li>
      `).join('');

      showcaseImg.style.opacity = '1';
      showcaseImg.style.transform = 'scale(1)';
    }, 140);
  }

  // Eventos en botones de tabs
  tabsContainer.querySelectorAll('.gallery-tab-btn').forEach((btn, idx) => {
    btn.addEventListener('click', () => setScreen(idx));
  });

  // Eventos en miniaturas inferiores
  if (thumbsContainer) {
    thumbsContainer.querySelectorAll('.thumb-card').forEach((thumb, idx) => {
      thumb.addEventListener('click', () => setScreen(idx));
    });
  }

  // Inicializar en la primera pantalla
  setScreen(0);
}

/**
 * Acordeón accesible para preguntas frecuentes
 */
function initFAQ() {
  const faqItems = document.querySelectorAll('.faq-item');

  faqItems.forEach(item => {
    const questionBtn = item.querySelector('.faq-question');
    const answer = item.querySelector('.faq-answer');

    if (!questionBtn || !answer) return;

    questionBtn.addEventListener('click', () => {
      const isOpen = item.classList.contains('open');

      // Cerrar otros si se desea comportamiento exclusivo
      faqItems.forEach(otherItem => {
        if (otherItem !== item && otherItem.classList.contains('open')) {
          otherItem.classList.remove('open');
          otherItem.querySelector('.faq-question')?.setAttribute('aria-expanded', 'false');
        }
      });

      // Alternar estado actual
      item.classList.toggle('open', !isOpen);
      questionBtn.setAttribute('aria-expanded', (!isOpen).toString());
    });
  });
}
