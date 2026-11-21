
const fasilitas = document.querySelector("#fasilitasId");
const lokasi = document.querySelector("#lokasi");

if (fasilitas && lokasi) {
  const tampilkanLokasi = () => {
    lokasi.value = fasilitas.selectedOptions[0]?.dataset.lokasi ?? "Pilih fasilitas untuk melihat lokasi";
  };
  fasilitas.addEventListener("change", tampilkanLokasi);
  tampilkanLokasi();
}

document.addEventListener("submit", (event) => {
  const pesan = event.submitter?.dataset.confirm ?? event.target.dataset.confirm;
  if (pesan && !window.confirm(pesan)) {
    event.preventDefault();
  }
});
