// UM Peduli: peningkatan progresif. Semua alur tetap berjalan tanpa JavaScript.
"use strict";

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
const kurangiGerak = () => window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const simpanSesi = (k, v) => { try { sessionStorage.setItem(k, v); } catch { /* penyimpanan diblokir */ } };
const bacaSesi = (k) => { try { return sessionStorage.getItem(k); } catch { return null; } };
const simpanLokal = (k, v) => { try { localStorage.setItem(k, v); } catch { /* penyimpanan diblokir */ } };
const bacaLokal = (k) => { try { return localStorage.getItem(k); } catch { return null; } };
const setelahFrame = (fn) => requestAnimationFrame(() => requestAnimationFrame(fn));
const UKURAN_MAKS = 5 * 1024 * 1024;

document.documentElement.classList.add("js");

/* ---------- Popover: notifikasi dan menu akun ---------- */
function tutupPop(pop, kembalikanFokus = false) {
  const tombol = $("button[aria-expanded]", pop);
  const panel = $(".pop-panel", pop);
  if (!panel || panel.hidden) return;
  tombol.setAttribute("aria-expanded", "false");
  panel.classList.remove("tampil");
  setTimeout(() => { panel.hidden = true; }, kurangiGerak() ? 0 : 130);
  if (kembalikanFokus) tombol.focus();
}
function bukaPop(pop) {
  $$("[data-pop]").forEach((lain) => lain !== pop && tutupPop(lain));
  const tombol = $("button[aria-expanded]", pop);
  const panel = $(".pop-panel", pop);
  panel.hidden = false;
  tombol.setAttribute("aria-expanded", "true");
  setelahFrame(() => panel.classList.add("tampil"));
}
$$("[data-pop]").forEach((pop) => {
  const tombol = $("button[aria-expanded]", pop);
  tombol.addEventListener("click", () => (tombol.getAttribute("aria-expanded") === "true" ? tutupPop(pop) : bukaPop(pop)));
  pop.addEventListener("keydown", (e) => {
    if (e.key === "Escape") { tutupPop(pop, true); return; }
    if (e.key !== "ArrowDown" && e.key !== "ArrowUp") return;
    const item = $$(".pop-panel a, .pop-panel button", pop);
    if (!item.length || $(".pop-panel", pop).hidden) return;
    e.preventDefault();
    const i = item.indexOf(document.activeElement);
    const berikut = e.key === "ArrowDown" ? (i + 1) % item.length : (i - 1 + item.length) % item.length;
    item[berikut].focus();
  });
});
document.addEventListener("click", (e) => {
  $$("[data-pop]").forEach((pop) => { if (!pop.contains(e.target)) tutupPop(pop); });
});

/* ---------- Toast setelah aksi berhasil ---------- */
function aturToast(toast) {
  let waktu;
  const sembunyikan = () => {
    toast.classList.remove("tampil");
    setTimeout(() => toast.remove(), kurangiGerak() ? 0 : 220);
  };
  const jadwalkan = () => { clearTimeout(waktu); waktu = setTimeout(sembunyikan, 5000); };
  setelahFrame(() => toast.classList.add("tampil"));
  jadwalkan();
  toast.addEventListener("mouseenter", () => clearTimeout(waktu));
  toast.addEventListener("mouseleave", jadwalkan);
  toast.addEventListener("focusin", () => clearTimeout(waktu));
  $("[data-tutup-toast]", toast)?.addEventListener("click", sembunyikan);
}
$$("[data-toast]").forEach(aturToast);

/* ---------- Tab: indikator bergeser + panah kiri/kanan ---------- */
function posisikanIndikator(tab, target, animasi = true) {
  const indikator = $(".tab-indikator", tab);
  const aktif = target || $('.tab-item[aria-current="page"]', tab);
  if (!indikator || !aktif) return;
  indikator.classList.toggle("tanpa-gerak", !animasi);
  indikator.style.setProperty("--tab-x", `${aktif.offsetLeft}px`);
  indikator.style.setProperty("--tab-s", `${aktif.offsetWidth / 100}`);
}
function aturTab(tab) {
  posisikanIndikator(tab, null, false);
  tab.addEventListener("keydown", (e) => {
    if (e.key !== "ArrowRight" && e.key !== "ArrowLeft") return;
    const item = $$(".tab-item", tab);
    const i = item.indexOf(document.activeElement);
    if (i < 0) return;
    e.preventDefault();
    item[(i + (e.key === "ArrowRight" ? 1 : -1) + item.length) % item.length].focus();
  });
}
$$(".tab").forEach(aturTab);
window.addEventListener("resize", () => $$(".tab").forEach((t) => posisikanIndikator(t, null, false)));

/* ---------- Daftar bersaring: debounce, filter, tab, paginasi tanpa muat ulang ---------- */
const wilayahAwal = $$("[data-wilayah]");
async function muatDaftar(url, { dorong = true } = {}) {
  const wilayah = $$("[data-wilayah]");
  wilayah.forEach((w) => w.setAttribute("aria-busy", "true"));
  try {
    const respons = await fetch(url, { headers: { "X-Requested-With": "fetch" }, credentials: "same-origin" });
    if (!respons.ok || respons.redirected) throw new Error(String(respons.status));
    const dok = new DOMParser().parseFromString(await respons.text(), "text/html");
    wilayah.forEach((lama) => {
      const baru = dok.getElementById(lama.id);
      if (!baru) return;
      if (lama.classList.contains("tab")) {
        // Elemen tab dipertahankan agar indikator bergeser; hanya isi dan status aktif yang diperbarui.
        const itemBaru = $$(".tab-item", baru);
        $$(".tab-item", lama).forEach((item, i) => {
          if (!itemBaru[i]) return;
          item.innerHTML = itemBaru[i].innerHTML;
          item.href = itemBaru[i].getAttribute("href");
          if (itemBaru[i].hasAttribute("aria-current")) item.setAttribute("aria-current", "page");
          else item.removeAttribute("aria-current");
        });
        posisikanIndikator(lama);
      } else {
        lama.replaceWith(baru);
      }
    });
    if (dorong) history.pushState({ daftar: true }, "", url);
    else history.replaceState({ daftar: true }, "", url);
  } catch {
    window.location.href = url;
  } finally {
    $$("[data-wilayah]").forEach((w) => w.removeAttribute("aria-busy"));
  }
}
function urlForm(form) {
  const data = new URLSearchParams(new FormData(form));
  for (const [k, v] of [...data]) if (!v) data.delete(k);
  return `${form.action.split("?")[0]}${data.toString() ? `?${data}` : ""}`;
}
if (wilayahAwal.length) {
  $$("form[data-saring]").forEach((form) => {
    let tunda;
    form.addEventListener("input", (e) => {
      if (e.target.type !== "search") return;
      clearTimeout(tunda);
      tunda = setTimeout(() => muatDaftar(urlForm(form), { dorong: false }), 300);
    });
    form.addEventListener("change", (e) => { if (e.target.matches("[data-saring-langsung]")) muatDaftar(urlForm(form)); });
    form.addEventListener("submit", (e) => { e.preventDefault(); muatDaftar(urlForm(form)); });
  });
  document.addEventListener("click", (e) => {
    const tautan = e.target.closest(".tab-item, .paginasi a[href]");
    if (!tautan || e.metaKey || e.ctrlKey || e.shiftKey || e.button !== 0) return;
    e.preventDefault();
    const tab = tautan.closest(".tab");
    if (tab) posisikanIndikator(tab, tautan);
    muatDaftar(tautan.href);
  });
  window.addEventListener("popstate", () => muatDaftar(location.href, { dorong: false }));
}
$$("[data-saring-otomatis] [data-saring-langsung]").forEach((sel) => sel.addEventListener("change", () => sel.form.submit()));

/* ---------- Dialog: modal verifikasi admin dan tambah petugas ---------- */
let pemicuDialog = null;
function tutupDialog(dialog) {
  dialog.classList.remove("tampil");
  const selesai = () => {
    if (dialog.open) dialog.close();
    if (dialog.dataset.sementara) dialog.remove();
    pemicuDialog?.focus();
    pemicuDialog = null;
  };
  if (kurangiGerak()) selesai(); else setTimeout(selesai, 200);
}
function aturDialog(dialog) {
  dialog.addEventListener("cancel", (e) => { e.preventDefault(); tutupDialog(dialog); });
  dialog.addEventListener("click", (e) => {
    if (e.target === dialog || e.target.closest("[data-tutup-modal]")) tutupDialog(dialog);
  });
}
function bukaDialog(dialog, pemicu) {
  pemicuDialog = pemicu || document.activeElement;
  dialog.showModal();
  setelahFrame(() => dialog.classList.add("tampil"));
}
$$("dialog.modal").forEach((d) => {
  aturDialog(d);
  if (d.dataset.bukaOtomatis) bukaDialog(d);
});
$$("[data-buka-dialog]").forEach((tombol) => tombol.addEventListener("click", () => {
  const dialog = document.getElementById(tombol.dataset.bukaDialog);
  if (dialog) bukaDialog(dialog, tombol);
}));

async function bukaModalLaporan(tautan) {
  const dialog = document.createElement("dialog");
  dialog.className = "modal";
  dialog.dataset.sementara = "ya";
  dialog.setAttribute("aria-labelledby", "judul-modal");
  dialog.innerHTML = '<div class="modal-isi" aria-busy="true"><h2 id="judul-modal" class="sr-only">Memuat detail laporan…</h2>'
    + '<div class="skeleton" aria-hidden="true"><span></span><span></span><span></span><span></span></div></div>';
  $("#modal-akar").append(dialog);
  aturDialog(dialog);
  bukaDialog(dialog, tautan);
  const url = new URL(tautan.href, location.href);
  url.searchParams.set("modal", "1");
  try {
    const respons = await fetch(url, { credentials: "same-origin" });
    if (!respons.ok || respons.redirected) throw new Error(String(respons.status));
    dialog.innerHTML = await respons.text();
    aturFormDi(dialog);
    $("[data-tutup-modal]", dialog)?.focus();
  } catch {
    dialog.innerHTML = '<div class="modal-isi"><header class="modal-kepala"><h2 id="judul-modal">Detail laporan gagal dimuat</h2>'
      + '<button class="tombol-ikon tombol-kotak" type="button" data-tutup-modal aria-label="Tutup">×</button></header>'
      + '<div class="modal-badan"><p>Periksa koneksi, lalu coba lagi, atau <a class="tautan"></a>.</p></div></div>';
    const cadangan = $(".modal-badan a", dialog);
    cadangan.href = tautan.href;
    cadangan.textContent = "buka halaman detail";
    $("[data-tutup-modal]", dialog).focus();
  }
}
document.addEventListener("click", (e) => {
  const tautan = e.target.closest("a[data-modal]");
  if (tautan) {
    if (e.metaKey || e.ctrlKey || e.shiftKey || e.button !== 0) return;
    e.preventDefault();
    bukaModalLaporan(tautan);
    return;
  }
  const baris = e.target.closest(".tabel tbody tr");
  if (baris && !e.target.closest("a, button, input, select") && !window.getSelection()?.toString()) {
    const utama = $("a[data-modal]", baris);
    if (utama) bukaModalLaporan(utama);
  }
});
$$(".tabel tbody tr").forEach((tr) => { if ($("a[data-modal]", tr)) tr.dataset.barisTautan = ""; });

/* ---------- Form: validasi per kolom, fokus ke galat pertama, tombol memproses ---------- */
function galatUntuk(kontrol) {
  const id = (kontrol.getAttribute("aria-describedby") || "").split(" ").find((x) => x.endsWith("-galat"));
  return id ? document.getElementById(id) : null;
}
function tandai(kontrol, pesan) {
  const galat = galatUntuk(kontrol);
  const field = kontrol.closest(".field");
  if (pesan) {
    kontrol.setAttribute("aria-invalid", "true");
    field?.classList.add("field-galat");
    if (galat) galat.textContent = pesan;
  } else {
    kontrol.removeAttribute("aria-invalid");
    field?.classList.remove("field-galat");
    if (galat) galat.textContent = "";
  }
}
function periksaBerkas(berkas) {
  if (!berkas) return null;
  if (!["image/jpeg", "image/png"].includes(berkas.type)) return "Format foto harus JPG atau PNG.";
  if (berkas.size > UKURAN_MAKS) return "Foto maksimal 5 MB, format JPG atau PNG. Pilih foto yang lebih kecil.";
  return null;
}
function aturFormDi(root) {
  $$("form[data-validasi], form[id='form-tindak'], form[data-panel-tugaskan]", root).forEach((form) => {
    if (form.dataset.siap) return;
    form.dataset.siap = "ya";
    form.addEventListener("input", (e) => { if (e.target.matches("input, textarea, select")) tandai(e.target, ""); });
    form.addEventListener("change", (e) => { if (e.target.matches("select")) tandai(e.target, ""); });
    form.addEventListener("submit", (e) => {
      const pemicu = e.submitter;
      const selesai = pemicu?.hasAttribute("data-selesai") || ($("#status", form)?.value === "SELESAI" && form.hasAttribute("data-progres"));
      const salah = [];
      $$("input, select, textarea", form).forEach((k) => {
        if (k.type === "hidden" || k.disabled) return;
        let pesan = "";
        if (k.type === "file") {
          const wajib = k.required || (selesai && form.hasAttribute("data-progres"));
          pesan = periksaBerkas(k.files[0]) || (wajib && !k.files.length ? (selesai ? "Unggah foto hasil perbaikan sebelum menandai selesai." : k.dataset.pesan) : "");
        } else if (!k.checkValidity()) {
          pesan = k.validity.typeMismatch ? (k.dataset.pesan || k.validationMessage) : (k.dataset.pesan || k.validationMessage);
        }
        tandai(k, pesan);
        if (pesan) salah.push(k);
      });
      if (salah.length) {
        e.preventDefault();
        const pertama = salah[0];
        (pertama.type === "file" ? pertama.closest(".dropzone")?.querySelector("label") || pertama : pertama).focus();
        if (pertama.type === "file") pertama.focus();
        return;
      }
      if (form.dataset.konfirmasi && !window.confirm(form.dataset.konfirmasi)) { e.preventDefault(); return; }
      const tombol = pemicu || $("button[type='submit']", form) || document.querySelector(`button[form='${form.id}']`);
      if (tombol) {
        tombol.setAttribute("aria-busy", "true");
        if (tombol.dataset.memproses) tombol.lastChild.textContent = tombol.dataset.memproses;
      }
      setTimeout(() => {
        $$("button[type='submit']", form).concat(form.id ? $$(`button[form='${form.id}']`) : []).forEach((b) => { b.disabled = true; });
      }, 0);
    });
  });
}
aturFormDi(document);
$$("form[data-konfirmasi]:not([data-validasi])").forEach((form) => form.addEventListener("submit", (e) => {
  if (!window.confirm(form.dataset.konfirmasi)) e.preventDefault();
}));

/* ---------- Dropzone foto ---------- */
$$("[data-dropzone]").forEach((zona) => {
  const input = $(".dropzone-input", zona);
  const kosong = $(".dropzone-kosong", zona);
  const isi = $(".dropzone-isi", zona);
  const wadahPratinjau = $("[data-pratinjau]", zona);
  let urlPratinjau;
  const tampilkan = () => {
    const berkas = input.files[0];
    const salah = periksaBerkas(berkas);
    if (urlPratinjau) URL.revokeObjectURL(urlPratinjau);
    wadahPratinjau.replaceChildren();
    if (!berkas || salah) {
      if (salah) { input.value = ""; tandai(input, salah); }
      kosong.hidden = false; isi.hidden = true; zona.classList.remove("berisi");
      return;
    }
    tandai(input, "");
    urlPratinjau = URL.createObjectURL(berkas);
    const img = new Image(104, 80);
    img.alt = `Pratinjau ${berkas.name}`;
    img.src = urlPratinjau;
    wadahPratinjau.append(img);
    $("[data-nama-berkas]", zona).textContent = berkas.name;
    const mb = berkas.size / 1048576;
    $("[data-ukuran-berkas]", zona).textContent = `1 foto · ${mb >= 1 ? `${mb.toFixed(1).replace(".", ",")} MB` : `${Math.max(1, Math.round(berkas.size / 1024))} KB`} · Siap dikirim`;
    kosong.hidden = true; isi.hidden = false; zona.classList.add("berisi");
  };
  input.addEventListener("change", tampilkan);
  ["dragenter", "dragover"].forEach((ev) => zona.addEventListener(ev, (e) => { e.preventDefault(); zona.classList.add("seret"); }));
  ["dragleave", "dragend"].forEach((ev) => zona.addEventListener(ev, () => zona.classList.remove("seret")));
  zona.addEventListener("drop", (e) => {
    e.preventDefault();
    zona.classList.remove("seret");
    if (!e.dataTransfer?.files?.length) return;
    const dt = new DataTransfer();
    dt.items.add(e.dataTransfer.files[0]);
    input.files = dt.files;
    tampilkan();
  });
  $("[data-hapus-berkas]", zona)?.addEventListener("click", () => { input.value = ""; tampilkan(); input.focus(); });
});

/* ---------- Gedung mengikuti fakultas ---------- */
$$("select[data-gedung-untuk]").forEach((gedung) => {
  const fakultas = document.getElementById(gedung.dataset.gedungUntuk);
  const saring = () => {
    $$("option[data-fakultas]", gedung).forEach((o) => {
      const cocok = !fakultas.value || o.dataset.fakultas === fakultas.value;
      o.hidden = !cocok; o.disabled = !cocok;
    });
    if (gedung.selectedOptions[0]?.disabled) gedung.value = "";
  };
  fakultas.addEventListener("change", saring);
  saring();
});

/* ---------- Panel "Tugaskan laporan": ringkasan laporan terpilih ---------- */
$$("[data-panel-tugaskan]").forEach((panel) => {
  const pilih = $("#kasusId", panel);
  const ringkas = $("[data-ringkas-laporan]", panel);
  const kosong = ringkas.innerHTML;
  const isi = () => {
    const o = pilih.selectedOptions[0];
    if (!o || !o.value) { ringkas.innerHTML = kosong; return; }
    const strong = document.createElement("strong");
    strong.textContent = o.dataset.fakultas;
    const lokasi = document.createElement("p");
    lokasi.className = "ringkas-lokasi";
    lokasi.textContent = o.dataset.lokasi;
    const deskripsi = document.createElement("p");
    deskripsi.className = "ringkas-deskripsi";
    deskripsi.textContent = o.dataset.deskripsi;
    ringkas.replaceChildren(strong, lokasi, deskripsi);
  };
  pilih.addEventListener("change", isi);
  isi();
});

/* ---------- Grafik tumbuh dan donut tersapu sekali per sesi ---------- */
const grafik = $("[data-grafik]");
const donut = $("[data-donut]");
if ((grafik || donut) && !bacaSesi("grafik-dilihat") && !kurangiGerak()) {
  grafik?.classList.add("siap-tumbuh");
  donut?.classList.add("siap-sapu");
  setelahFrame(() => {
    grafik?.classList.replace("siap-tumbuh", "tumbuh");
    donut?.classList.replace("siap-sapu", "sapu");
  });
}
if (grafik || donut) simpanSesi("grafik-dilihat", "ya");

/* ---------- Linimasa terisi bila tahap bertambah sejak kunjungan terakhir ---------- */
const linimasa = $(".linimasa[data-tahap]");
if (linimasa) {
  const kunci = `tahap:${location.pathname}`;
  const sebelumnya = Number(bacaLokal(kunci) || 0);
  const kini = Number(linimasa.dataset.tahap);
  if (sebelumnya && kini > sebelumnya) linimasa.classList.add("isi-baru");
  simpanLokal(kunci, String(kini));
}
