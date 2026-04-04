/**
 * Full-width closing CTA section. The button smooth-scrolls back to the search
 * panel at the top of the page.
 */

export function FinalCTASection() {
  function handleClick() {
    document.getElementById("hero")?.scrollIntoView({ behavior: "smooth" });
  }

  return (
    <section
      className="py-24 text-center"
      style={{ background: "var(--ch-navy)" }}
    >
      <div className="max-w-2xl mx-auto px-6">
        <h2
          className="text-4xl font-semibold text-white mb-4"
          style={{ fontFamily: "var(--font-display)" }}
        >
          Ready to stop guessing?
        </h2>
        <p className="text-lg mb-10" style={{ color: "rgba(255,255,255,0.65)" }}>
          Your next multi-city trip starts with one search.
        </p>
        <button
          type="button"
          onClick={handleClick}
          className="inline-flex items-center gap-2 px-8 py-4 rounded-xl text-base font-bold transition-transform hover:scale-105 active:scale-95"
          style={{ background: "var(--ch-accent)", color: "#0F1F3D" }}
        >
          Find My Route →
        </button>
      </div>
    </section>
  );
}
