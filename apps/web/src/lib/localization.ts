export type Locale = 'en'|'hi'|'ar';
export const localeLabels:Record<Locale,string>={en:'English',hi:'हिन्दी',ar:'العربية'};
export const messages = {
 en:{browse:'Browse',forYou:'For You',movies:'Movies',series:'Series',plans:'Plans',myList:'My List',collections:'Collections',channels:'Channels',party:'Watch Party',profiles:'My profiles',explore:'Explore catalog',more:'More',language:'Language'},
 hi:{browse:'ब्राउज़',forYou:'आपके लिए',movies:'फ़िल्में',series:'सीरीज़',plans:'प्लान',myList:'मेरी सूची',collections:'संग्रह',channels:'चैनल',party:'वॉच पार्टी',profiles:'मेरी प्रोफ़ाइल',explore:'कैटलॉग देखें',more:'और',language:'भाषा'},
 ar:{browse:'تصفح',forYou:'لك',movies:'أفلام',series:'مسلسلات',plans:'الخطط',myList:'قائمتي',collections:'مجموعات',channels:'قنوات',party:'مشاهدة جماعية',profiles:'ملفاتي',explore:'استكشف المحتوى',more:'المزيد',language:'اللغة'}
} satisfies Record<Locale,Record<string,string>>;
export function isLocale(value:string|null):value is Locale{return value==='en'||value==='hi'||value==='ar'}
export function direction(locale:Locale){return locale==='ar'?'rtl':'ltr'}
