
import HomeActions from "./components/HomeActions";
import Header from "./components/Header";

const steps = [
  {
    number: "01",
    emoji: "🎁",
    title: "선물 고르기",
    description: "받고 싶은 선물을 위시리스트에 담아요.",
  },
  {
    number: "02",
    emoji: "💌",
    title: "마음 모으기",
    description: "친구들이 원하는 금액만큼 참여해요.",
  },
  {
    number: "03",
    emoji: "✨",
    title: "선물 완성",
    description: "목표 금액을 모으면 선물이 완성돼요.",
  },
];

export default function Home() {
  return (
      <main className="min-h-screen bg-[#f7f9f8]">
        <div className="mx-auto min-h-screen max-w-md bg-white shadow-sm">
          <Header />

          <section className="px-6 pb-10 pt-12">
          <span className="inline-flex rounded-full bg-[#e9f7f4] px-3 py-2 text-xs font-semibold text-[#328c82]">
            함께하는 선물의 새로운 방법
          </span>

            <h1 className="mt-5 text-[32px] font-extrabold leading-[1.4] tracking-tight text-[#17212b]">
              갖고 싶은 선물,
              <br />
              <span className="text-[#328c82]">친구들과 함께</span>
              <br />
              만들어 보세요.
            </h1>

            <p className="mt-5 text-sm leading-7 text-gray-500">
              부담은 나누고, 기쁨은 더 크게.
              <br />
              원하는 만큼 마음을 모아 선물해요.
            </p>

            <div className="mt-8">
              <HomeActions />
            </div>
          </section>

          <section className="px-6 pb-12">
            <div className="mb-5 flex items-end justify-between">
              <div>
                <p className="text-xs font-semibold text-[#328c82]">
                  GIFT TOGETHER
                </p>
                <h2 className="mt-2 text-xl font-bold text-gray-900">
                  함께 모으면 가능한 선물
                </h2>
              </div>
              <span className="text-xs text-gray-400">이용 예시</span>
            </div>

            <div className="overflow-hidden rounded-3xl border border-[#dcefeb] bg-[#f0faf7] p-6">
              <div className="flex items-center gap-4">
                <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl bg-white text-4xl shadow-sm">
                  🎧
                </div>
                <div>
                  <p className="text-xs text-gray-500">
                    친구들과 함께 모으는 선물
                  </p>
                  <h3 className="mt-2 text-lg font-bold text-gray-900">
                    에어팟 프로
                  </h3>
                  <p className="mt-1 text-xs text-[#328c82]">
                    친구 2명이 참여했어요
                  </p>
                </div>
              </div>

              <div className="mt-7 flex items-end justify-between">
                <div>
                  <p className="text-xs text-gray-500">현재 모인 금액</p>
                  <p className="mt-1 text-2xl font-extrabold text-[#328c82]">
                    80,000원
                  </p>
                </div>
                <p className="pb-1 text-sm text-gray-500">
                  목표 150,000원
                </p>
              </div>

              <div
                  role="progressbar"
                  aria-label="예시 펀딩 진행률"
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={53}
                  className="mt-4 h-3 overflow-hidden rounded-full bg-white"
              >
                <div className="h-full w-[53.33%] rounded-full bg-[#65b9ad]" />
              </div>

              <div className="mt-3 flex items-center justify-between text-xs">
              <span className="font-semibold text-[#328c82]">
                53% 달성
              </span>
                <span className="text-gray-500">
                70,000원 남았어요
              </span>
              </div>

              <div className="mt-6 rounded-2xl bg-white/80 p-4">
                <p className="text-sm font-medium text-gray-700">
                  💌 친구들이 마음을 보탰어요
                </p>
                <p className="mt-2 text-xs leading-6 text-gray-500">
                  한 친구는 30,000원, 다른 친구는 50,000원.
                  <br />
                  함께 모아 선물을 완성해요.
                </p>
              </div>
            </div>
          </section>

          <section className="border-t border-gray-100 px-6 py-12">
            <p className="text-xs font-semibold text-[#328c82]">
              HOW IT WORKS
            </p>
            <h2 className="mt-2 text-xl font-bold text-gray-900">
              선물하는 방법도 간단해요
            </h2>
            <p className="mt-2 text-sm text-gray-500">
              단 세 단계로 함께 선물할 수 있어요.
            </p>

            <div className="mt-7 space-y-5">
              {steps.map((step) => (
                  <div key={step.number} className="flex items-center gap-4">
                    <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-[#e9f7f4] text-2xl">
                      {step.emoji}
                    </div>
                    <div className="flex-1">
                      <p className="text-xs font-semibold text-[#328c82]">
                        STEP {step.number}
                      </p>
                      <h3 className="mt-1 text-sm font-bold text-gray-900">
                        {step.title}
                      </h3>
                      <p className="mt-1 text-xs leading-5 text-gray-500">
                        {step.description}
                      </p>
                    </div>
                  </div>
              ))}
            </div>
          </section>

          <footer className="border-t border-gray-100 px-6 py-8 text-center text-xs text-gray-400">
            Gift Together · 함께하는 선물의 즐거움
          </footer>
        </div>
      </main>
  );
}
