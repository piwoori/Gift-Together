import HomeActions from "./components/HomeActions";
import Header from "./components/Header";

export default function Home() {
  return (
      <main className="min-h-screen bg-[#f8faf9]">
        <div className="mx-auto max-w-md min-h-screen bg-white shadow-sm">
          <Header />

          <section className="px-6 pt-16 pb-12">
          <span className="text-sm font-semibold text-[#328c82]">
            마음을 모아 선물하는 새로운 방법
          </span>

            <h1 className="mt-4 text-3xl font-bold leading-snug text-gray-900">
              갖고 싶은 선물,
              <br />
              이제 함께 선물해요 🎁
            </h1>

            <p className="mt-5 text-sm leading-7 text-gray-500">
              한 사람이 선물 금액을 전부 부담하지 않아도 괜찮아요.
              <br />
              친구들과 원하는 만큼 마음을 모아
              <br />
              소중한 사람에게 선물해 보세요.
            </p>

            <div className="mt-10 rounded-3xl bg-[#e9f7f4] p-6">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-gray-600">
                  함께 모으는 선물
                </p>

                <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold text-[#328c82]">
                    이용 예시
                </span>
              </div>

              <h2 className="mt-2 text-xl font-bold text-gray-900">
                에어팟 프로
              </h2>

              <div className="mt-7 flex items-end justify-between">
              <span className="text-2xl font-bold text-[#328c82]">
                80,000원
              </span>
                <span className="text-sm text-gray-500">
                / 150,000원
              </span>
              </div>

              <div className="mt-3 h-3 overflow-hidden rounded-full bg-white">
                <div className="h-full w-[53.33%] rounded-full bg-[#65b9ad]"/>
              </div>

              <p className="mt-3 text-right text-xs text-gray-500">
                목표까지 70,000원 남았어요
              </p>

              <p className="mt-5 border-t border-[#cce8e2] pt-4 text-xs leading-5 text-gray-500">
                친구들이 각각 30,000원과 50,000원을 보탰어요.
                <br/>
                목표 금액이 모이면 선물을 받을 수 있어요.
              </p>
              
            </div>

            <HomeActions/>
          </section>

          <footer className="px-6 py-8 text-center text-xs text-gray-400">
            Gift Together · 함께하는 선물의 즐거움
          </footer>
        </div>
      </main>
  );
}