// Peningkatan progresif kecil. Semua alur utama tetap berjalan tanpa JavaScript.

// Formulir laporan: tampilkan gedung dan ruang dari fasilitas yang dipilih.
const fasilitas = document.querySelector("#fasilitasId");
const lokasi = document.querySelector("#lokasi");
if (fasilitas && lokasi) {
  const kosong = lokasi.textContent;
  const tampilkanLokasi = () => {
    lokasi.textContent = fasilitas.selectedOptions[0]?.dataset.lokasi ?? kosong;
  };
  fasilitas.addEventListener("change", tampilkanLokasi);
  tampilkanLokasi();
}

// Konfirmasi untuk aksi yang menutup kasus atau memindahkan laporan.
document.addEventListener("submit", (event) => {
  const pesan = event.submitter?.dataset.confirm ?? event.target.dataset.confirm;
  if (pesan && !window.confirm(pesan)) {
    event.preventDefault();
  }
});
