/**
 * yNotes — Script Principal (v1.0.20 Oficial)
 * Funcionalidad: 
 * - Mockup 3D interactivo con giro táctil/click y auto-rotación
 * - Lightbox HD para visualizar capturas a resolución nativa completa
 * - Galería interactiva con información detallada de cada pantalla
 * - Control de tema claro/oscuro
 * - Menú responsive móvil
 * - Scroll Reveal animado
 * 100% Autónomo, sin dependencias externas ni telemetría
 */

document.addEventListener('DOMContentLoaded', () => {
  // 1. GESTIÓN DE TEMA (CLARO / OSCURO)
  initTheme();

  // 2. HEADER Y NAVEGACIÓN MÓVIL
  initNavigation();

  // 3. ANIMACIONES DE SCROLL REVEAL
  initScrollReveal();

  // 4. MOCKUP 3D INTERACTIVO EN HERO (GIRAR PARA VER MÁS)
  initHeroRotator();

  // 5. LIGHTBOX MODAL HD PANTALLA COMPLETA
  initLightbox();

  // 6. GALERÍA DE CAPTURAS INFERIOR
  initGallery();

  // 7. ACORDEÓN DE PREGUNTAS FRECUENTES (FAQ)
  initFAQ();
});

/**
 * Control del Modo Oscuro / Claro con persistencia en localStorage
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

  if (urlTheme === 'dark' || urlTheme === 'light') {
    applyTheme(urlTheme);
  } else if (storedTheme === 'dark' || storedTheme === 'light') {
    applyTheme(storedTheme);
  } else {
    applyTheme(systemPrefersDark.matches ? 'dark' : 'light');
  }

  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => {
      const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
      const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
      applyTheme(newTheme);
      localStorage.setItem('ynotes_theme', newTheme);
    });
  }

  systemPrefersDark.addEventListener('change', (e) => {
    if (!localStorage.getItem('ynotes_theme')) {
      applyTheme(e.matches ? 'dark' : 'light');
    }
  });
}

/**
 * Control del header y menú móvil responsive
 */
function initNavigation() {
  const header = document.querySelector('.site-header');
  const mobileToggle = document.getElementById('mobileMenuToggle');
  const navMenu = document.getElementById('navMenu');

  window.addEventListener('scroll', () => {
    if (window.scrollY > 20) {
      header?.classList.add('scrolled');
    } else {
      header?.classList.remove('scrolled');
    }
  }, { passive: true });

  if (mobileToggle && navMenu) {
    mobileToggle.addEventListener('click', () => {
      const isOpen = navMenu.classList.toggle('open');
      mobileToggle.setAttribute('aria-expanded', isOpen.toString());
    });

    navMenu.querySelectorAll('.nav-link').forEach(link => {
      link.addEventListener('click', () => {
        navMenu.classList.remove('open');
        mobileToggle.setAttribute('aria-expanded', 'false');
      });
    });

    document.addEventListener('click', (e) => {
      if (!navMenu.contains(e.target) && !mobileToggle.contains(e.target) && navMenu.classList.contains('open')) {
        navMenu.classList.remove('open');
        mobileToggle.setAttribute('aria-expanded', 'false');
      }
    });
  }
}

/**
 * Animaciones suaves de scroll
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
    revealElements.forEach(el => el.classList.add('revealed'));
  }
}

/**
 * Lista de capturas HD oficiales (v1.0.20)
 */
const heroScreens = [
  {
    title: '1/5 · Principal AMOLED',
    badgeTitle: 'Edición AMOLED',
    badgeSub: 'v1.0.20 Oficial',
    src: 'assets/screenshots/v1020-01-dashboard-amoled.webp?v=1.0.20_hd',
    alt: 'yNotes v1.0.20 Pantalla Principal AMOLED con tarjetas vivas y botón degradado'
  },
  {
    title: '2/5 · Bóveda AES-256',
    badgeTitle: 'Bóveda Segura',
    badgeSub: 'Cifrado por Hardware',
    src: 'assets/screenshots/v1020-02-vault-safezone.webp?v=1.0.20_hd',
    alt: 'yNotes v1.0.20 Zona Segura y Bóveda Cifrada'
  },
  {
    title: '3/5 · Google Notes',
    badgeTitle: 'Tema Google Notes',
    badgeSub: 'Estilo Personalizable',
    src: 'assets/screenshots/v1020-03-theme-google.webp?v=1.0.20_hd',
    alt: 'yNotes v1.0.20 Estilo visual Google Notes con botón flotante clásico'
  },
  {
    title: '4/5 · Modo Reversa',
    badgeTitle: 'Modo Reversa',
    badgeSub: 'Protección Anti-Intrusos',
    src: 'assets/screenshots/v1020-04-safezone-settings.webp?v=1.0.20_hd',
    alt: 'yNotes v1.0.20 Ajustes de Zona Segura y Modo Reversa'
  },
  {
    title: '5/5 · Ajustes v1.0.20',
    badgeTitle: 'Ajustes Oficiales',
    badgeSub: 'Versión 1.0.20 HD',
    src: 'assets/screenshots/v1020-05-settings-v1020.webp?v=1.0.20_hd',
    alt: 'yNotes v1.0.20 Ajustes Generales, Sonidos y Selector de Temas'
  }
];

/**
 * MOCKUP 3D INTERACTIVO (GIRA EL TELÉFONO PARA VER LAS DEMÁS)
 */
function initHeroRotator() {
  const frame = document.getElementById('heroPhoneFrame');
  const img = document.getElementById('heroPhoneImg');
  const title = document.getElementById('heroScreenTitle');
  const badgeTitle = document.getElementById('heroBadgeTitle');
  const badgeSub = document.getElementById('heroBadgeSub');
  const dots = document.querySelectorAll('.hero-dot');
  const prevBtn = document.getElementById('heroPrevBtn');
  const nextBtn = document.getElementById('heroNextBtn');
  const wrap = document.getElementById('heroPhoneWrap');

  if (!frame || !img) return;

  let currentIndex = 0;
  let isFlipping = false;
  let autoTimer = null;

  function rotateTo(newIndex, direction = 'next') {
    if (isFlipping) return;
    isFlipping = true;

    if (newIndex < 0) newIndex = heroScreens.length - 1;
    if (newIndex >= heroScreens.length) newIndex = 0;

    const flipClass = direction === 'next' ? 'flipping-next' : 'flipping-prev';
    frame.classList.add(flipClass);

    setTimeout(() => {
      currentIndex = newIndex;
      const screen = heroScreens[currentIndex];
      img.src = screen.src;
      img.alt = screen.alt;

      if (title) title.textContent = screen.title;
      if (badgeTitle) badgeTitle.textContent = screen.badgeTitle;
      if (badgeSub) badgeSub.textContent = screen.badgeSub;

      dots.forEach((dot, i) => {
        dot.classList.toggle('active', i === currentIndex);
        dot.setAttribute('aria-selected', (i === currentIndex).toString());
      });

      frame.classList.remove(flipClass);
      setTimeout(() => {
        isFlipping = false;
      }, 250);
    }, 180);
  }

  function next() { rotateTo(currentIndex + 1, 'next'); }
  function prev() { rotateTo(currentIndex - 1, 'prev'); }

  if (nextBtn) nextBtn.addEventListener('click', (e) => { e.stopPropagation(); next(); resetTimer(); });
  if (prevBtn) prevBtn.addEventListener('click', (e) => { e.stopPropagation(); prev(); resetTimer(); });

  dots.forEach(dot => {
    dot.addEventListener('click', (e) => {
      e.stopPropagation();
      const target = parseInt(dot.getAttribute('data-index') || '0', 10);
      if (target !== currentIndex) {
        rotateTo(target, target > currentIndex ? 'next' : 'prev');
        resetTimer();
      }
    });
  });

  // Swipe táctil en móvil
  let touchStartX = 0;
  let touchEndX = 0;

  wrap.addEventListener('touchstart', (e) => {
    touchStartX = e.changedTouches[0].screenX;
  }, { passive: true });

  wrap.addEventListener('touchend', (e) => {
    touchEndX = e.changedTouches[0].screenX;
    const diff = touchEndX - touchStartX;
    if (Math.abs(diff) > 40) {
      if (diff < 0) next();
      else prev();
      resetTimer();
    }
  }, { passive: true });

  // Inclinación 3D en escritorio al pasar el ratón
  wrap.addEventListener('mousemove', (e) => {
    if (isFlipping) return;
    const rect = wrap.getBoundingClientRect();
    const x = e.clientX - rect.left - rect.width / 2;
    const y = e.clientY - rect.top - rect.height / 2;
    const rotX = -(y / rect.height) * 14;
    const rotY = (x / rect.width) * 18;
    frame.style.transform = `perspective(1000px) rotateX(${rotX}deg) rotateY(${rotY}deg) scale(1.02)`;
  });

  wrap.addEventListener('mouseleave', () => {
    if (isFlipping) return;
    frame.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale(1)';
  });

  // Rotación automática suave
  function startTimer() {
    autoTimer = setInterval(next, 4800);
  }
  function resetTimer() {
    clearInterval(autoTimer);
    startTimer();
  }

  wrap.addEventListener('mouseenter', () => clearInterval(autoTimer));
  wrap.addEventListener('mouseleave', () => resetTimer());

  startTimer();
}

/**
 * MODAL LIGHTBOX HD PANTALLA COMPLETA
 */
function initLightbox() {
  const lightbox = document.getElementById('hdLightbox');
  const lightboxImg = document.getElementById('lightboxImg');
  const lightboxTitle = document.getElementById('lightboxTitle');
  const lightboxDownload = document.getElementById('lightboxDownload');
  const closeBtn = document.getElementById('lightboxCloseBtn');
  const overlay = document.getElementById('lightboxOverlay');

  if (!lightbox || !lightboxImg) return;

  function openLightbox(src, title) {
    lightboxImg.src = src;
    if (lightboxTitle) lightboxTitle.textContent = title || 'Captura en Alta Definición (HD)';
    if (lightboxDownload) {
      lightboxDownload.href = src;
      lightboxDownload.setAttribute('download', (title ? title.replace(/[^a-zA-Z0-9]/g, '-').toLowerCase() : 'ynotes-hd') + '.webp');
    }
    lightbox.classList.add('active');
    document.body.style.overflow = 'hidden';
  }

  function closeLightbox() {
    lightbox.classList.remove('active');
    document.body.style.overflow = '';
  }

  if (closeBtn) closeBtn.addEventListener('click', closeLightbox);
  if (overlay) overlay.addEventListener('click', closeLightbox);
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && lightbox.classList.contains('active')) {
      closeLightbox();
    }
  });

  const heroZoomBtn = document.getElementById('heroZoomBtn');
  const heroImg = document.getElementById('heroPhoneImg');
  if (heroZoomBtn && heroImg) {
    heroZoomBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      openLightbox(heroImg.src, document.getElementById('heroScreenTitle')?.textContent || 'Captura HD');
    });
  }

  const galleryImg = document.getElementById('galleryImg');
  if (galleryImg) {
    galleryImg.style.cursor = 'zoom-in';
    galleryImg.addEventListener('click', () => {
      const activeTitle = document.getElementById('galleryTitle')?.textContent || 'Captura de yNotes';
      openLightbox(galleryImg.src, activeTitle);
    });
  }
}

/**
 * GALERÍA INFERIOR INTERACTIVA
 */
const galleryScreens = [
  {
    id: 'dashboard',
    title: 'Pantalla Principal AMOLED Glass (v1.0.20)',
    subtitle: 'Notas organizadas con máxima legibilidad, estética AMOLED pura y alto contraste.',
    src: 'assets/screenshots/v1020-01-dashboard-amoled.webp?v=1.0.20_hd',
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
    src: 'assets/screenshots/v1020-02-vault-safezone.webp?v=1.0.20_hd',
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
    src: 'assets/screenshots/v1020-03-theme-google.webp?v=1.0.20_hd',
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
    src: 'assets/screenshots/v1020-04-safezone-settings.webp?v=1.0.20_hd',
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
    src: 'assets/screenshots/v1020-05-settings-v1020.webp?v=1.0.20_hd',
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

    tabsContainer.querySelectorAll('.gallery-tab-btn').forEach((btn, i) => {
      btn.classList.toggle('active', i === index);
      btn.setAttribute('aria-selected', (i === index).toString());
    });

    if (thumbsContainer) {
      thumbsContainer.querySelectorAll('.thumb-card').forEach((thumb, i) => {
        thumb.classList.toggle('active', i === index);
      });
    }

    showcaseImg.style.opacity = '0.3';
    showcaseImg.style.transform = 'scale(0.98)';

    setTimeout(() => {
      showcaseImg.src = screen.src;
      showcaseImg.alt = screen.alt;
      showcaseTitle.textContent = screen.title;
      showcaseSub.textContent = screen.subtitle;

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

  tabsContainer.querySelectorAll('.gallery-tab-btn').forEach((btn, idx) => {
    btn.addEventListener('click', () => setScreen(idx));
  });

  if (thumbsContainer) {
    thumbsContainer.querySelectorAll('.thumb-card').forEach((thumb, idx) => {
      thumb.addEventListener('click', () => setScreen(idx));
    });
  }

  setScreen(0);
}

/**
 * Acordeón accesible para preguntas frecuentes (FAQ)
 */
function initFAQ() {
  const faqItems = document.querySelectorAll('.faq-item');

  faqItems.forEach(item => {
    const questionBtn = item.querySelector('.faq-question');
    const answer = item.querySelector('.faq-answer');

    if (!questionBtn || !answer) return;

    questionBtn.addEventListener('click', () => {
      const isOpen = item.classList.contains('open');

      faqItems.forEach(otherItem => {
        if (otherItem !== item && otherItem.classList.contains('open')) {
          otherItem.classList.remove('open');
          otherItem.querySelector('.faq-question')?.setAttribute('aria-expanded', 'false');
        }
      });

      item.classList.toggle('open', !isOpen);
      questionBtn.setAttribute('aria-expanded', (!isOpen).toString());
    });
  });
}
