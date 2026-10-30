document.addEventListener('DOMContentLoaded', () => {
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Smooth reveal for cards and sections.
    if (!reduced && 'IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('is-visible');
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.08 });

        document.querySelectorAll('.feature-card, .stat-card, .job-card, .section-block, .panel').forEach(el => {
            el.classList.add('scroll-reveal');
            observer.observe(el);
        });
    }

    // A very subtle cursor glow on desktop screens.
    if (!reduced && window.matchMedia('(pointer:fine)').matches) {
        const glow = document.createElement('div');
        glow.className = 'cursor-glow';
        document.body.appendChild(glow);

        let x = -200, y = -200;
        document.addEventListener('mousemove', event => {
            x = event.clientX;
            y = event.clientY;
            glow.style.transform = `translate3d(${x}px, ${y}px, 0)`;
        }, { passive: true });
    }
});
