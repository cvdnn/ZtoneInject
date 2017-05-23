package android.inject;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class InjectFinder {
    private static final String TAG = "InjectFinder";

    public static <A extends AppCompatActivity> boolean injectOptionsMenu(A a, Menu menu) {

        return injectOptionsMenu(null, a, menu);
    }

    public static <A extends AppCompatActivity> boolean injectOptionsMenu(Class<?> clazz, A a, Menu menu) {

        return injectOptionsMenu(clazz, a, a.getMenuInflater(), menu);
    }

    public static <O> boolean injectOptionsMenu(O o, MenuInflater menuInflater, Menu menu) {

        return injectOptionsMenu(null, o, menuInflater, menu);
    }

    public static <O> boolean injectOptionsMenu(Class<?> clazz, O o, MenuInflater menuInflater, Menu menu) {
        boolean result = false;
        if (menu != null) {
            int menuId = findMenuId(clazz != null ? clazz : (o != null ? o.getClass() : null));
            if (menuId != 0) {
                if (menuInflater != null) {
                    menuInflater.inflate(menuId, menu);
                    result = true;
                }
            }
        }

        return result;
    }

    private static int findMenuId(Class<?> clazz) {
        int menuResId = 0;

        if (clazz != null) {
            FindMenu fileMenu = clazz.getAnnotation(FindMenu.class);
            if (fileMenu != null) {
                menuResId = fileMenu.value();
            }
        }

        return menuResId;
    }

    /**
     * inject view
     *
     * @param o
     */
    public static <O> void injectView(@NonNull Context context, O o) {

        injectView(context, null, o);
    }

    /**
     * inject view
     *
     * @param clazz
     * @param o
     */
    public static <O> void injectView(@NonNull Context context, Class<?> clazz, O o) {
        Class<?> tempClazz = clazz != null ? clazz : (o != null ? o.getClass() : null);
        if (context != null && tempClazz != null) {
            // find view
            final SparseArray<View> tempViewArray = new SparseArray<View>();
            Field[] fields = tempClazz.getDeclaredFields();
            if (fields != null && fields.length > 0) {
                for (Field field : fields) {
                    FindView viewInject = field.getAnnotation(FindView.class);
                    if (viewInject != null) {
                        try {
                            int viewId = findViewId(context, viewInject);
                            View view = findViewById(o, viewId, viewInject.parent());
                            if (view != null) {
                                // Check if the object type is match
                                Class<?> targetType = field.getType();
                                Class<?> viewType = view.getClass();
                                if (!targetType.isAssignableFrom(viewType)) {
                                    String err = "Type mismatch! \n" +
                                            "  The view is   (" + viewType.getName() + ") R.id." + view.getContext().getResources().getResourceEntryName(viewId) + "#" + String.format("0x%08x", viewId) + "\n" +
                                            "  Cannot set to (" + targetType.getName() + ") " + o.getClass().getName() + "." + field.getName();
                                    Log.e(TAG, err);
                                    continue;
                                }

                                // 设置变量值
                                if (setField(o, field, view)) {
                                    tempViewArray.append(viewId, view);
                                }
                            }

                        } catch (Throwable e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }
                }
            }

            // 获取onClicklistener类
            // 获取注解的View id
            int[] clickIds = findClickIds(tempClazz);
            if (clickIds != null && clickIds.length > 0 && o instanceof View.OnClickListener) {
                View.OnClickListener clickListener = (View.OnClickListener) o;

                for (int id : clickIds) {
                    if (id != 0) {
                        View tempView = tempViewArray.get(id);
                        if (tempView == null) {
                            try {
                                tempView = findViewById(o, id, 0);
                            } catch (Throwable t) {
                                Log.e(TAG, t.getMessage(), t);
                            }
                        }

                        // 设置点击事件
                        if (tempView != null) {
                            tempView.setOnClickListener(clickListener);
                        }
                    }
                }
            }
        }
    }

    private static <O> boolean setField(O o, Field field, View view) {
        boolean result = false;
        if (field != null && o != null && view != null) {
            try {
                field.setAccessible(true);
                field.set(o, view);
                result = true;
            } catch (Throwable e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        return result;
    }

    private static Class<?> getListenerClazz(Class<? extends Annotation> annClazz) {
        Class<?> clazz = null;
        if (annClazz != null) {
            Listener listener = annClazz.getAnnotation(Listener.class);
            if (listener != null) {
                clazz = listener.value();
            }
        }

        return clazz;
    }

    private static <O> int[] findClickIds(Class<O> clazz) {
        int[] clickIds = null;

        if (clazz != null) {
            SetOnClickListener setOnClickListener = clazz.getAnnotation(SetOnClickListener.class);
            if (setOnClickListener != null) {
                clickIds = setOnClickListener.value();
            }
        }

        return clickIds;
    }

    private static int findViewId(@NonNull Context context, FindView viewInject) {
        int id = 0;

        if (viewInject != null) {
            id = viewInject.value();
            if (id == 0) {
                String resName = viewInject.name();
                if (!TextUtils.isEmpty(resName)) {
                    id = getId(context, resName);
                }
            }
        }

        return id;
    }

    private static <O> View findViewById(O o, int id, int parent) throws Throwable {
        View view = null;

        if (o != null) {
            if (o instanceof Activity) {
                Activity activity = (Activity) o;
                View parentView = null;
                if (parent != 0) {
                    parentView = activity.findViewById(parent);
                }

                if (parentView != null) {
                    view = parentView.findViewById(id);
                } else {
                    view = activity.findViewById(id);
                }
            } else if (o instanceof View) {
                View tempView = (View) o;
                View parentView = null;
                if (parent != 0) {
                    parentView = tempView.findViewById(parent);
                }

                if (parentView != null) {
                    view = parentView.findViewById(id);
                } else {
                    view = tempView.findViewById(id);
                }
            } else if (o instanceof Fragment) {
                Fragment fragment = (Fragment) o;
                View fragmentView = fragment.getView();
                if (fragmentView != null) {
                    View parentView = null;
                    if (parent != 0) {
                        parentView = fragmentView.findViewById(parent);
                    }

                    if (parentView != null) {
                        view = parentView.findViewById(id);
                    } else {
                        view = fragmentView.findViewById(id);
                    }
                }
            } else if (o instanceof OnViewFinder) {
                view = findViewById(((OnViewFinder) o).getView(), id, parent);
            }
        }

        return view;
    }

    private static int getId(@NonNull Context context, @NonNull String idName) {

        return getIdentifier(context, idName, "id");
    }

    private static int getIdentifier(@NonNull Context context, @NonNull String resName, String type) {

        return context.getResources().getIdentifier(resName, type, context.getPackageName());
    }
}
