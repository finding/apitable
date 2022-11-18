import { colorVars, useThemeColors } from '@apitable/components';
import { ISelectFieldOption, Strings, t } from '@apitable/core';
import { useUnmount } from 'ahooks';
import { Input } from 'antd';
import cls from 'classnames';
import { SubscribeGrade, SubscribeLabel } from 'pc/components/subscribe_system/subscribe_label';
import { useResponsive } from 'pc/hooks';
import { stopPropagation } from 'pc/utils';
import * as React from 'react';
import { useState } from 'react';
import DeleteIcon from 'static/icon/common/common_icon_delete.svg';
import { ScreenSize } from '../component_display/enum';
import { Modal } from '../mobile/modal';
import { ColorGroup } from './color_group';
import { OptionSetting } from './enum';
import styles from './style.module.less';
// @ts-ignore
import { SubscribeUsageTipType, triggerUsageAlert } from 'enterprise';

export interface IColorPickerPane {
  option: ISelectFieldOption;
  showRenameInput?: boolean;
  onChange?: (type: OptionSetting, id: string, value: string | number) => void;
  onClose: () => void;
}

export const ColorPickerPane: React.FC<IColorPickerPane> = props => {
  const { option, showRenameInput = false, onChange, onClose } = props;
  const [newName, setNewName] = useState(option.name);
  const colors = useThemeColors();

  const renderMenu = (title: string, colorGroup: number[], showTag?: boolean, isBase?: boolean) => (
    <div className={cls(styles.menu, { [styles.bg]: showTag })}>
      <div
        className={cls(styles.menuTitle, {
          [styles.base]: isBase,
        })}
      >
        <div
          style={{
            fontWeight: showTag ? 'bold' : 'normal',
            color: colorVars.firstLevelText,
          }}
        >
          {title}
        </div>
        {showTag && <SubscribeLabel grade={SubscribeGrade.Silver} />}
      </div>
      <ColorGroup
        colorGroup={colorGroup}
        option={option}
        onChange={(type: OptionSetting, id: string, value: string | number) => {
          if (title === t(Strings.option_configuration_advance_palette)) {
            const result = triggerUsageAlert('rainbowLabel', { alwaysAlert: true }, SubscribeUsageTipType.Alert);
            if (result) return;
          }
          onChange?.(type, id, value);
          onClose();
        }}
      />
    </div>
  );

  const onInput = e => {
    e.stopPropagation();
    const value = e.target.value;
    if (value.length > 100) {
      return;
    }
    setNewName(value);
  };

  const closeAndSave = () => {
    if (newName !== option.name && newName.trim().length > 0) {
      onChange!(OptionSetting.RENAME, option.id, newName);
      onClose();
    }
  };

  useUnmount(() => {
    closeAndSave();
  });

  const { screenIsAtMost } = useResponsive();
  const isMobile = screenIsAtMost(ScreenSize.md);

  const onDelete = () => {
    if (isMobile) {
      Modal.warning({
        title: t(Strings.delete),
        content: t(Strings.warning_confirm_to_del_option),
        onOk: () => onChange!(OptionSetting.DELETE, option.id, option.id),
      });
      return;
    }
    onChange!(OptionSetting.DELETE, option.id, option.id);
  };

  return (
    <div className={styles.pane}>
      {showRenameInput && (
        <>
          <div className={styles.editor} onClick={stopPropagation}>
            <Input
              size={isMobile ? 'large' : 'small'}
              onChange={onInput}
              onPressEnter={e => {
                e.stopPropagation();
                closeAndSave();
              }}
              defaultValue={option.name}
              onMouseMove={e => stopPropagation((e as any) as React.MouseEvent)}
              value={newName}
            />
            <div className={styles.deleteIconWrap}>
              <DeleteIcon width={16} height={16} fill={colors.thirdLevelText} onClick={onDelete} />
            </div>
          </div>
          <div className={styles.divider} />
        </>
      )}
      <div className={styles.colorMenuGroup}>
        {renderMenu(
          t(Strings.option_configuration_basic_palette),
          Array.from({ length: 20 }, (item, index) => index),
          false,
          true,
        )}
        {renderMenu(
          t(Strings.option_configuration_advance_palette),
          Array.from({ length: 30 }, (item, index) => index + 20),
          true,
        )}
      </div>
    </div>
  );
};
